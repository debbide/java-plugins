import os
import shutil
import subprocess

def run_git(args, cwd):
    print(f"执行命令: git {' '.join(args)}")
    res = subprocess.run(["git"] + args, cwd=cwd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
    if res.returncode != 0:
        print(f"  错误: {res.stderr.strip()}")
    else:
        print(f"  成功: {res.stdout.strip()[:100]}...")
    return res

def main():
    plugin_dir = r"e:\ck\docker\bungeecord-main\java-plugins-main"
    remote_url = "https://github.com/debbide/java-plugins"
    
    # 物理清理
    dot_git = os.path.join(plugin_dir, ".git")
    if os.path.exists(dot_git):
        print(f"正在删除旧的 .git 目录: {dot_git}")
        shutil.rmtree(dot_git, ignore_errors=True)
    
    # 重新开始
    print("\n--- 正在重新初始化仓库 ---")
    run_git(["init"], plugin_dir)
    run_git(["branch", "-M", "main"], plugin_dir)
    
    # 设置远程
    print(f"\n--- 正在关联远程仓库: {remote_url} ---")
    run_git(["remote", "add", "origin", remote_url], plugin_dir)
    
    # 提交
    print("\n--- 正在提交核心代码 ---")
    run_git(["add", "."], plugin_dir)
    run_git(["commit", "-m", "feat: implement port hijacking and splitter logic (initial clean push)"], plugin_dir)
    
    # 推送
    print("\n--- 正在推送到 main 分支 ---")
    res = run_git(["push", "-u", "origin", "main", "--force"], plugin_dir)
    
    if res.returncode == 0:
        print("\n[大获全胜] 插件已精准推送到 main 分支！")
    else:
        print("\n[警告] 推送可能遇到了问题，请检查 SSH 权限或仓库访问设置。")

if __name__ == "__main__":
    main()
