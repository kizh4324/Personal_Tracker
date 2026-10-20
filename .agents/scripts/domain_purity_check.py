#!/usr/bin/env python3
"""
Domain Purity & UI Token Conformance Checker
Enforces:
- AD-1: com.personaltracker.domain must be 100% pure Kotlin (zero android.* or androidx.* imports).
- AD-7: ui/components and ui/screens must have zero hardcoded hex colors (must resolve exclusively from theme tokens).

Can be executed directly via CLI or integrated into Antigravity PreToolUse/PostToolUse hooks with --hook.
"""

import sys
import os
import re
import json
from pathlib import Path

def find_repo_root() -> Path:
    """Finds the root repository directory containing ARCHITECTURE-SPINE.md or .git."""
    current = Path(__file__).resolve().parent
    for parent in [current] + list(current.parents):
        if (parent / "ARCHITECTURE-SPINE.md").exists() or (parent / ".git").exists():
            return parent
    return Path.cwd()

def check_domain_purity(repo_root: Path) -> list:
    """Verifies that no android.* or androidx.* imports exist in the domain layer."""
    domain_path = repo_root / "app" / "src" / "main" / "java" / "com" / "personaltracker" / "domain"
    violations = []

    if not domain_path.exists():
        return violations

    import_pattern = re.compile(r'^\s*import\s+(android\.|androidx\.)')

    for root, _, files in os.walk(domain_path):
        for file in files:
            if file.endswith(".kt") or file.endswith(".java"):
                file_path = Path(root) / file
                rel_path = file_path.relative_to(repo_root)
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    for line_num, line in enumerate(f, start=1):
                        if import_pattern.search(line):
                            violations.append({
                                "rule": "AD-1 (Domain Purity)",
                                "file": str(rel_path).replace("\\", "/"),
                                "line": line_num,
                                "text": line.strip()
                            })
    return violations

def check_ui_token_conformance(repo_root: Path) -> list:
    """Verifies that ui/components and ui/screens contain zero hardcoded hex colors."""
    ui_paths = [
        repo_root / "app" / "src" / "main" / "java" / "com" / "personaltracker" / "ui" / "components",
        repo_root / "app" / "src" / "main" / "java" / "com" / "personaltracker" / "ui" / "screens",
    ]
    violations = []

    # Detect Color(0x...), Color(0X...), android parseColor, or hex color string literals "#FFFFFF" / "#FFFFFFFF"
    hex_patterns = [
        re.compile(r'\bColor\s*\(\s*0x[0-9a-fA-F]+\b', re.IGNORECASE),
        re.compile(r'\bparseColor\s*\(\s*"#[0-9a-fA-F]{6,8}"\s*\)', re.IGNORECASE),
        re.compile(r'(?<![a-zA-Z0-9_])"#[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?"(?![a-zA-Z0-9_])'),
    ]

    for ui_dir in ui_paths:
        if not ui_dir.exists():
            continue
        for root, _, files in os.walk(ui_dir):
            for file in files:
                if file.endswith(".kt") or file.endswith(".java"):
                    file_path = Path(root) / file
                    rel_path = file_path.relative_to(repo_root)
                    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                        for line_num, line in enumerate(f, start=1):
                            stripped = line.strip()
                            if stripped.startswith("//") or stripped.startswith("*"):
                                continue
                            for pattern in hex_patterns:
                                if pattern.search(line):
                                    violations.append({
                                        "rule": "AD-7 (UI Token Conformance)",
                                        "file": str(rel_path).replace("\\", "/"),
                                        "line": line_num,
                                        "text": line.strip()
                                    })
                                    break
    return violations

FILE_MUTATION_TOOLS = {
    "write_to_file",
    "replace_file_content",
    "multi_replace_file_content",
    "edit_file",
    "write_file",
}

def run_all_checks(repo_root: Path) -> tuple:
    domain_violations = check_domain_purity(repo_root)
    ui_violations = check_ui_token_conformance(repo_root)
    all_violations = domain_violations + ui_violations
    return all_violations, domain_violations, ui_violations

def main():
    repo_root = find_repo_root()
    is_hook = "--hook" in sys.argv

    hook_data = {}
    if is_hook:
        try:
            stdin_content = sys.stdin.read()
            if stdin_content.strip():
                hook_data = json.loads(stdin_content)
        except Exception as e:
            sys.stderr.write(f"Warning parsing hook stdin: {e}\n")

    all_violations, domain_violations, ui_violations = run_all_checks(repo_root)

    if is_hook:
        tool_call = hook_data.get("toolCall", {})
        tool_name = tool_call.get("name", "")

        # PostToolUse contract expects empty dict {}
        # PreToolUse contract expects {"decision": ..., "allow_tool": ...}
        if len(all_violations) == 0:
            if "stepIdx" in hook_data and tool_name in FILE_MUTATION_TOOLS:
                print(json.dumps({}))
            else:
                print(json.dumps({"decision": "allow", "allow_tool": True}))
            sys.exit(0)
        else:
            summary = f"Detected {len(all_violations)} architectural violations: " + "; ".join(
                f"{v['rule']} in {v['file']}:{v['line']}" for v in all_violations[:3]
            )
            if "stepIdx" in hook_data and tool_name in FILE_MUTATION_TOOLS:
                sys.stderr.write(f"Architecture Warning: {summary}\n")
                print(json.dumps({}))
                sys.exit(0)
            else:
                print(json.dumps({
                    "decision": "deny",
                    "allow_tool": False,
                    "reason": summary
                }))
                sys.exit(0)

    # CLI Execution Mode
    print(f"Personal-Tracker Architectural Purity Linter")
    print(f"Repository Root: {repo_root}")
    print("--------------------------------------------------")

    if len(domain_violations) == 0:
        print("[PASS] AD-1 Domain Purity: Zero android.* or androidx.* imports in domain/")
    else:
        print(f"[FAIL] AD-1 Domain Purity: {len(domain_violations)} forbidden imports found:")
        for v in domain_violations:
            print(f"       -> {v['file']}:{v['line']} | {v['text']}")

    if len(ui_violations) == 0:
        print("[PASS] AD-7 UI Token Conformance: Zero hardcoded hex colors in ui/components/ and ui/screens/")
    else:
        print(f"[FAIL] AD-7 UI Token Conformance: {len(ui_violations)} hardcoded hex colors found:")
        for v in ui_violations:
            print(f"       -> {v['file']}:{v['line']} | {v['text']}")

    print("--------------------------------------------------")
    if len(all_violations) == 0:
        print("Verdict: ALL ARCHITECTURAL PURITY CHECKS PASSED.")
        sys.exit(0)
    else:
        print(f"Verdict: FAILED with {len(all_violations)} total violation(s).")
        sys.exit(1)

if __name__ == "__main__":
    main()
