#!/usr/bin/env python3
"""
Harness Safety Gate Hook (PreToolUse)
Inspects tool calls before execution to prevent destructive actions,
avoid PowerShell binary corruption traps, and enforce confirmation on dangerous git operations.
"""

import sys
import json
import re

def evaluate_command(command_line: str) -> dict:
    cmd = command_line.strip()

    # a) PowerShell binary corruption trap with adb screencap redirection
    # e.g., adb exec-out screencap -p > image.png or adb shell screencap -p > image.png
    if re.search(r'\badb\b.*(?:\bexec-out\b|\bshell\b).*screencap.*>', cmd, re.IGNORECASE) or \
       re.search(r'\badb\b.*screencap.*>', cmd, re.IGNORECASE):
        return {
            "decision": "deny",
            "reason": (
                "PowerShell binary redirection trap detected! Redirecting 'adb exec-out screencap -p > ...' "
                "creates corrupted UTF-16LE files on Windows. "
                "Enforce two-step pull: 'adb shell screencap -p /sdcard/temp.png; adb pull /sdcard/temp.png <destination>'."
            )
        }

    # a) Destructive system / filesystem commands
    destructive_patterns = [
        (r'\brm\s+-(?:r[fF]|fr)\s+/', "Root filesystem deletion blocked ('rm -rf /')."),
        (r'\brm\s+-(?:r[fF]|fr)\s+.*\.git\b', "VCS repository deletion blocked ('rm -rf .git')."),
        (r'\bRemove-Item\b.*(?:-Recurse.*-Force|-Force.*-Recurse)', "Destructive recursive force deletion blocked ('Remove-Item -Recurse -Force')."),
        (r'\bformat\s+[A-Za-z]:', "Disk format command blocked ('format <drive>:')."),
        (r'^\s*format\b(?!\s*--)', "Disk format command blocked ('format')."),
        (r'\bmkfs\b', "Filesystem creation blocked ('mkfs')."),
        (r'\b(?:rd|rmdir)\s+/(?:s|S)\s+/(?:q|Q)\s+.*\.git\b', "Git directory purge blocked ('rmdir /s /q .git')."),
        (r'\bdel\s+/(?:f|F)\s+/(?:s|S)\s+/(?:q|Q)\s+.*\.git\b', "Git directory purge blocked ('del /f /s /q .git')."),
    ]

    for pattern, reason in destructive_patterns:
        if re.search(pattern, cmd, re.IGNORECASE):
            return {
                "decision": "deny",
                "reason": f"Destructive command blocked by Harness safety gate: {reason}"
            }

    # b) Force push, hard reset, clean -fd
    force_ask_patterns = [
        (r'\bgit\s+push\b.*(?:\s--force\b|\s-f\b|\s--force-with-lease\b)', "Git force push requested ('git push --force'). User confirmation required."),
        (r'\bgit\s+reset\b.*--hard\b', "Git hard reset requested ('git reset --hard'). Uncommitted changes will be permanently lost."),
        (r'\bgit\s+clean\b.*-(?:fd|df|f)\b', "Destructive git clean requested ('git clean -fd'). Untracked files will be permanently removed."),
    ]

    for pattern, reason in force_ask_patterns:
        if re.search(pattern, cmd, re.IGNORECASE):
            return {
                "decision": "force_ask",
                "reason": reason
            }

    # c) Standard build, test, python, git, adb, and workspace utility commands
    return {"decision": "allow"}

def run_self_tests() -> int:
    tests = [
        # Destructive (must deny)
        ("rm -rf /", "deny"),
        ("rm -rf .git", "deny"),
        ("Remove-Item -Recurse -Force node_modules", "deny"),
        ("format C:", "deny"),
        ("mkfs.ext4 /dev/sda1", "deny"),
        ("adb exec-out screencap -p > screen.png", "deny"),
        ("adb shell screencap -p > screen.png", "deny"),
        ("& adb exec-out screencap -p > out.png", "deny"),

        # High-risk git (must force_ask)
        ("git push --force origin main", "force_ask"),
        ("git push -f", "force_ask"),
        ("git push origin main --force-with-lease", "force_ask"),
        ("git reset --hard HEAD~1", "force_ask"),
        ("git reset --hard", "force_ask"),
        ("git clean -fd", "force_ask"),
        ("git clean -f", "force_ask"),

        # Standard safe commands (must allow)
        ("./gradlew test", "allow"),
        (".\\gradlew lintDebug", "allow"),
        ("gradlew.bat assembleDebug", "allow"),
        ("python .agents/scripts/domain_purity_check.py", "allow"),
        ("python3 scripts/test.py", "allow"),
        ("git status", "allow"),
        ("git commit -m 'feat: test'", "allow"),
        ("git push origin main", "allow"),
        ("git reset HEAD~1", "allow"),
        ("git log --pretty=format:'%h %s'", "allow"),
        ("adb shell screencap -p /sdcard/temp.png; adb pull /sdcard/temp.png screen.png", "allow"),
        ("adb devices", "allow"),
    ]

    failed = 0
    print(f"Running {len(tests)} harness gate self-tests...")
    for cmd, expected in tests:
        res = evaluate_command(cmd)
        actual = res.get("decision")
        if actual != expected:
            print(f"[FAIL] Cmd: '{cmd}' | Expected: {expected} | Actual: {actual}")
            failed += 1
        else:
            print(f"[PASS] {expected.upper():<9} -> {cmd[:65]}")

    if failed == 0:
        print(f"\nAll {len(tests)} self-tests passed cleanly.")
        return 0
    else:
        print(f"\n{failed} self-tests failed!")
        return 1

def main():
    # If invoked with --hook or hook mode is active
    if "--hook" in sys.argv:
        try:
            raw_input = sys.stdin.read()
            if not raw_input.strip():
                print(json.dumps({"decision": "allow"}))
                sys.exit(0)

            data = json.loads(raw_input)
            tool_call = data.get("toolCall", {})
            tool_name = tool_call.get("name", "")
            args = tool_call.get("args", {})

            if tool_name == "run_command":
                cmd = args.get("CommandLine") or args.get("commandLine") or args.get("command", "")
                result = evaluate_command(cmd)
                print(json.dumps(result))
                sys.exit(0)

            # Non run_command tool calls are allowed by default
            print(json.dumps({"decision": "allow"}))
            sys.exit(0)
        except Exception as e:
            sys.stderr.write(f"Harness gate error: {e}\n")
            print(json.dumps({"decision": "ask", "reason": f"Harness gate input parsing error: {e}"}))
            sys.exit(0)

    # If invoked with --test
    if len(sys.argv) > 1 and sys.argv[1] == "--test":
        sys.exit(run_self_tests())

    # If single command argument provided directly
    if len(sys.argv) > 1 and sys.argv[1] not in ("--test", "--hook"):
        cmd_arg = " ".join(sys.argv[1:])
        result = evaluate_command(cmd_arg)
        print(json.dumps(result, indent=2))
        sys.exit(0 if result["decision"] in ("allow", "force_ask") else 1)

    # Standalone execution without arguments runs tests
    sys.exit(run_self_tests())

if __name__ == "__main__":
    main()
