#!/usr/bin/env node

const { execFileSync } = require("node:child_process");
const fs = require("node:fs");

function git(args, options = {}) {
  return execFileSync("git", args, {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "inherit"],
    ...options,
  });
}

function normalizePath(file) {
  return file.replace(/\\/g, "/");
}

function splitNullSeparated(output) {
  return output.split("\0").map(normalizePath).filter(Boolean);
}

function getStagedFiles() {
  return splitNullSeparated(
    git(["diff", "--cached", "--name-only", "-z", "--diff-filter=ACMR"]),
  );
}

function getWorkingTreeFiles() {
  return splitNullSeparated(
    git(["ls-files", "--cached", "--others", "--exclude-standard", "-z"]),
  );
}

function isIgnoredDocumentation(file) {
  return (
    file.startsWith("docs/") ||
    file.startsWith(".github/") ||
    file.startsWith(".husky/") ||
    file.endsWith(".md") ||
    file === ".gitmessage.txt"
  );
}

function isPackageManifest(file) {
  return /(^|\/)(package(-lock)?\.json|pnpm-lock\.yaml|yarn\.lock)$/.test(file);
}

function isFrontendSourceOrConfig(file) {
  return (
    (file.startsWith("frontend/") && /\.[cm]?[jt]sx?$|\.vue$/.test(file)) ||
    /(^|\/)vite\.config\.[cm]?[jt]s$/.test(file)
  );
}

function isBackendSourceOrBuild(file) {
  return (
    (file.startsWith("backend/") &&
      /(^|\/)(pom\.xml|build\.gradle(\.kts)?|settings\.gradle(\.kts)?|gradle\.properties|.*\.java|.*\.xml|.*\.toml)$/.test(
        file,
      )) ||
    /^(pom\.xml|build\.gradle(\.kts)?|settings\.gradle(\.kts)?|gradle\.properties|gradle\/.*\.toml)$/.test(
      file,
    )
  );
}

const rules = [
  {
    name: "React dependency",
    appliesTo: (file) =>
      isPackageManifest(file) || isFrontendSourceOrConfig(file),
    contentPattern:
      /("|'|`)(react|react-dom|@vitejs\/plugin-react)("|'|`)|from\s+("|'|`)react("|'|`)/i,
    message:
      "Frontend는 Vue.js를 사용해야 하므로 React 관련 의존성 또는 import를 추가할 수 없습니다.",
  },
  {
    name: "Spring Boot",
    appliesTo: isBackendSourceOrBuild,
    contentPattern: /spring-boot|org\.springframework\.boot/i,
    message:
      "Backend는 Spring Framework legacy를 사용해야 하므로 Spring Boot를 추가할 수 없습니다.",
  },
  {
    name: "JPA",
    appliesTo: isBackendSourceOrBuild,
    contentPattern:
      /JpaRepository|@Entity\b|javax\.persistence|jakarta\.persistence|hibernate-entitymanager|spring-data-jpa/i,
    message:
      "Persistence는 MyBatis를 사용해야 하므로 JPA 관련 코드를 추가할 수 없습니다.",
  },
];

function findViolations(entries) {
  const violations = [];

  for (const { file, content } of entries) {
    if (isIgnoredDocumentation(file)) {
      continue;
    }

    for (const rule of rules) {
      if (rule.appliesTo(file) && rule.contentPattern.test(content)) {
        violations.push({ file, rule });
      }
    }
  }

  return violations;
}

function readStagedEntries() {
  return getStagedFiles()
    .filter(
      (file) =>
        !isIgnoredDocumentation(file) &&
        rules.some((rule) => rule.appliesTo(file)),
    )
    .map((file) => ({
      file,
      content: git(["show", `:${file}`]),
    }));
}

function readWorkingTreeEntries() {
  const entries = [];

  for (const file of getWorkingTreeFiles()) {
    if (isIgnoredDocumentation(file) || !fs.existsSync(file)) {
      continue;
    }

    if (!rules.some((rule) => rule.appliesTo(file))) {
      continue;
    }

    entries.push({
      file,
      content: fs.readFileSync(file, "utf8"),
    });
  }

  return entries;
}

function parseMode(args) {
  if (args.length !== 1 || !["--staged", "--all"].includes(args[0])) {
    throw new Error(
      "Use one mode: node scripts/check-project-guardrails.js --staged|--all",
    );
  }

  return args[0] === "--staged" ? "staged" : "all";
}

function printViolations(violations) {
  console.error("\n프로젝트 기술 제약 위반이 감지되었습니다.\n");
  for (const violation of violations) {
    console.error(`- ${violation.file}: ${violation.rule.name}`);
    console.error(`  ${violation.rule.message}`);
  }
  console.error("\n허용 기술: Vue.js, Spring Framework legacy, MyBatis\n");
}

function runGuardrails(mode) {
  const entries =
    mode === "staged" ? readStagedEntries() : readWorkingTreeEntries();
  const violations = findViolations(entries);

  if (violations.length > 0) {
    printViolations(violations);
    return 1;
  }

  const scope =
    mode === "staged" ? "staged index content" : "tracked and untracked files";
  console.log(`Project guardrail check passed (${scope}).`);
  return 0;
}

function main() {
  try {
    return runGuardrails(parseMode(process.argv.slice(2)));
  } catch (error) {
    console.error(error.message);
    return 1;
  }
}

if (require.main === module) {
  process.exitCode = main();
}

module.exports = {
  findViolations,
  isBackendSourceOrBuild,
  isFrontendSourceOrConfig,
  isIgnoredDocumentation,
  isPackageManifest,
  normalizePath,
  parseMode,
  splitNullSeparated,
};
