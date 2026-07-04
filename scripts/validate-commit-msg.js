#!/usr/bin/env node

const fs = require("node:fs");

const commitMessagePath = process.argv[2];

if (!commitMessagePath) {
  console.error("Commit message file path was not provided.");
  process.exit(1);
}

const rawMessage = fs.readFileSync(commitMessagePath, "utf8");
const header = rawMessage
  .split(/\r?\n/)
  .map((line) => line.trim())
  .find((line) => line.length > 0 && !line.startsWith("#"));

if (!header) {
  console.error("Commit message is empty.");
  process.exit(1);
}

const allowedPrefixes = [
  /^Merge\b/,
  /^Revert\b/,
  /^fixup! /,
  /^squash! /
];

if (allowedPrefixes.some((pattern) => pattern.test(header))) {
  process.exit(0);
}

const conventionalCommitPattern =
  /^(feat|fix|docs|style|refactor|test|chore|build|ci|perf|revert)!?: (.+)$/;

const scopePattern = /^(feat|fix|docs|style|refactor|test|chore|build|ci|perf|revert)\([a-z0-9.-]+\)!?: /;
const areaTagPattern = /^\[(FE|BE|DB|DOCS|GITHUB|HOOK|INFRA|COMMON)\]\s+.+/;

const errors = [];

if (header.length > 72) {
  errors.push("첫 줄은 72자 이내로 작성해주세요.");
}

const match = header.match(conventionalCommitPattern);

if (!match) {
  if (scopePattern.test(header)) {
    errors.push("scope 괄호는 사용하지 않고, 영역은 [FE] 같은 area tag로 표현해주세요.");
  }
  errors.push("형식은 <type>: [AREA] <summary> 이어야 합니다.");
} else {
  const [, , summary] = match;

  if (!areaTagPattern.test(summary)) {
    errors.push("summary는 [FE], [BE], [DB], [DOCS], [GITHUB], [HOOK], [INFRA], [COMMON] 중 하나로 시작해야 합니다.");
  }
}

if (errors.length > 0) {
  console.error("\n커밋 메시지 규칙을 지켜주세요.\n");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  console.error("\n예시:");
  console.error("  feat: [FE] 로그인 화면 라우팅 추가 (#12)");
  console.error("  fix: [BE] 세션 만료 처리 보정 (#23)");
  console.error("  docs: [GITHUB] 이슈 작성 가이드 추가 (#5)\n");
  process.exit(1);
}
