import os
import subprocess

def run_git(args, cwd):
    print(f"执行命令: git {' '.join(args)} 在 {cwd}")
    res = subprocess.run(["git"] + args, cwd=cwd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
    return res

def main():
    plugin_dir = r"e:\ck\docker\bungeecord-main\java-plugins-main"
    outer_dir = r"e:\ck\docker\bungeecord-main"
    remote_plugin = "https://github.com/debbide/java-plugins"
    remote_bungee = "https://github.com/debbide/bungeecord"

    # Step 1: 修复外层主仓库的远程地址
    print("\n--- 步骤 1: 修复主仓库远程地址 ---")
    run_git(["remote", "set-url", "origin", remote_bungee], outer_dir)

    # Step 2: 确保插件目录有独立的 .git
    print("\n--- 步骤 2: 初始化/验证独立插件仓库 ---")
    dot_git = os.path.join(plugin_dir, ".git")
    if not os.path.exists(dot_git):
        run_git(["init"], plugin_dir)
    
    # 获取当前分支名
    res = run_git(["rev-parse", "--abbrev-ref", "HEAD"], plugin_dir)
    branch = res.stdout.strip()
    if not branch or branch == "HEAD":
        # 如果是新初始化的，重命名为 main
        run_git(["branch", "-M", "main"], plugin_dir)
        branch = "main"
    print(f"当前分支: {branch}")

    # Step 3: 配置插件仓库远程并推送
    print("\n--- 步骤 3: 精准推送插件代码 ---")
    run_git(["remote", "remove", "origin"], plugin_dir) # 先移除旧的以防万全
    run_git(["remote", "add", "origin", remote_plugin], plugin_dir)
    
    run_git(["add", "."], plugin_dir)
    run_git(["commit", "-m", "feat: implement port hijacking and splitter logic (precise clean push)"], plugin_dir)
    
    # 强制推送覆盖之前错误的提交
    print(f"正在强制推送 {branch} 到 {remote_plugin}...")
    res = run_git(["push", "origin", branch, "--force"], plugin_dir)
    
    if res.returncode == 0:
        print("\n[成功] 插件代码已精准推送到 GitHub，错误文件已被覆盖。")
    else:
        print(f"\n[失败] 推送失败: {res.stderr}")

if __name__ == "__main__":
    main()
