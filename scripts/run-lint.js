#!/usr/bin/env node

const { spawnSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

const target = process.argv[2] || "all";
const rootDir = process.cwd();

function exists(relativePath) {
  return fs.existsSync(path.join(rootDir, relativePath));
}

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(path.join(rootDir, relativePath), "utf8"));
}

function logSkip(message) {
  console.log(`[lint] skip: ${message}`);
}

function run(command, args, options = {}) {
  console.log(`[lint] run: ${command} ${args.join(" ")}`);

  const result = spawnSync(command, args, {
    cwd: options.cwd || rootDir,
    stdio: "inherit",
    shell: false
  });

  if (result.error) {
    console.error(`[lint] failed to start: ${command}`);
    console.error(result.error.message);
    return 1;
  }

  return result.status || 0;
}

function runFrontendLint() {
  if (!exists("frontend/package.json")) {
    logSkip("frontend/package.json not found");
    return 0;
  }

  const frontendPackage = readJson("frontend/package.json");

  if (!frontendPackage.scripts || !frontendPackage.scripts.lint) {
    logSkip("frontend package does not define a lint script");
    return 0;
  }

  const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";
  return run(npmCommand, ["--prefix", "frontend", "run", "lint"]);
}

function getBackendGradleCommand() {
  const backendDir = path.join(rootDir, "backend");

  if (process.platform === "win32" && exists("gradlew.bat")) {
    return {
      command: path.join(rootDir, "gradlew.bat"),
      args: ["-p", "backend", "check"],
      cwd: rootDir
    };
  }

  if (process.platform !== "win32" && exists("gradlew")) {
    return {
      command: path.join(rootDir, "gradlew"),
      args: ["-p", "backend", "check"],
      cwd: rootDir
    };
  }

  if (process.platform === "win32" && exists("backend/gradlew.bat")) {
    return {
      command: path.join(backendDir, "gradlew.bat"),
      args: ["check"],
      cwd: backendDir
    };
  }

  if (process.platform !== "win32" && exists("backend/gradlew")) {
    return {
      command: path.join(backendDir, "gradlew"),
      args: ["check"],
      cwd: backendDir
    };
  }

  return {
    command: process.platform === "win32" ? "gradle.bat" : "gradle",
    args: ["-p", "backend", "check"],
    cwd: rootDir
  };
}

function runBackendLint() {
  const hasGradleBuild =
    exists("backend/build.gradle") || exists("backend/build.gradle.kts");

  if (!hasGradleBuild) {
    logSkip("backend Gradle build file not found");
    return 0;
  }

  const gradle = getBackendGradleCommand();
  return run(gradle.command, gradle.args, { cwd: gradle.cwd });
}

const runners = {
  frontend: runFrontendLint,
  fe: runFrontendLint,
  backend: runBackendLint,
  be: runBackendLint
};

if (target === "all") {
  const frontendExit = runFrontendLint();
  if (frontendExit !== 0) {
    process.exit(frontendExit);
  }

  process.exit(runBackendLint());
}

if (!runners[target]) {
  console.error(`Unknown lint target: ${target}`);
  console.error("Use one of: all, frontend, fe, backend, be");
  process.exit(1);
}

process.exit(runners[target]());

