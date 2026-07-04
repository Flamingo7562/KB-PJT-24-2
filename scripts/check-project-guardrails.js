#!/usr/bin/env node

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");

function git(args) {
  return execFileSync("git", args, {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "ignore"]
  }).trim();
}

function getStagedFiles() {
  const output = git(["diff", "--cached", "--name-only", "--diff-filter=ACMR"]);
  return output ? output.split(/\r?\n/).filter(Boolean) : [];
}

function isIgnoredDocumentation(file) {
  return (
    file.startsWith("docs/") ||
    file.startsWith(".github/") ||
    file.startsWith(".husky/") ||
    file === "README.md" ||
    file === ".gitmessage.txt"
  );
}

const rules = [
  {
    name: "React dependency",
    filePattern: /(^|\/)(package(-lock)?\.json|pnpm-lock\.yaml|yarn\.lock|vite\.config\.[cm]?[jt]s|.*\.[jt]sx?)$/,
    contentPattern: /("|'|`)(react|react-dom|@vitejs\/plugin-react)("|'|`)|from\s+("|'|`)react("|'|`)/i,
    message: "Frontend는 Vue.js를 사용해야 하므로 React 관련 의존성 또는 import를 추가할 수 없습니다."
  },
  {
    name: "Spring Boot",
    filePattern: /(^|\/)(pom\.xml|build\.gradle(\.kts)?|.*\.java|.*\.xml)$/,
    contentPattern: /spring-boot|org\.springframework\.boot/i,
    message: "Backend는 Spring Framework legacy를 사용해야 하므로 Spring Boot를 추가할 수 없습니다."
  },
  {
    name: "JPA",
    filePattern: /(^|\/)(pom\.xml|build\.gradle(\.kts)?|.*\.java|.*\.xml)$/,
    contentPattern: /JpaRepository|@Entity\b|javax\.persistence|jakarta\.persistence|hibernate-entitymanager|spring-data-jpa/i,
    message: "Persistence는 MyBatis를 사용해야 하므로 JPA 관련 코드를 추가할 수 없습니다."
  }
];

const violations = [];

for (const file of getStagedFiles()) {
  if (isIgnoredDocumentation(file) || !fs.existsSync(file)) {
    continue;
  }

  const normalizedFile = file.replace(/\\/g, "/");
  const content = fs.readFileSync(file, "utf8");

  for (const rule of rules) {
    if (rule.filePattern.test(normalizedFile) && rule.contentPattern.test(content)) {
      violations.push({ file, rule });
    }
  }
}

if (violations.length > 0) {
  console.error("\n프로젝트 기술 제약 위반이 감지되었습니다.\n");
  for (const violation of violations) {
    console.error(`- ${violation.file}: ${violation.rule.name}`);
    console.error(`  ${violation.rule.message}`);
  }
  console.error("\n허용 기술: Vue.js, Spring Framework legacy, MyBatis\n");
  process.exit(1);
}

console.log("Project guardrail check passed.");

