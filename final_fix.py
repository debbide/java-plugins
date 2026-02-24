import os
import subprocess

def run_git(args, cwd):
    print(f"执行: git {' '.join(args)}")
    res = subprocess.run(["git"] + args, cwd=cwd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
    return res

def main():
    plugin_dir = r"e:\ck\docker\bungeecord-main\java-plugins-main"
    remote_url = "https://github.com/debbide/java-plugins"
    
    # 确保进入插件目录
    os.chdir(plugin_dir)
    
    # 1. 如果没有 .git，初始化一个
    if not os.path.exists(".git"):
        run_git(["init"], plugin_dir)
    
    # 2. 移除并重新设置远程
    run_git(["remote", "remove", "origin"], plugin_dir)
    run_git(["remote", "add", "origin", remote_url], plugin_dir)
    
    # 3. 强制切换并合并为新的 main 分支内容
    run_git(["checkout", "-b", "main_temp"], plugin_dir) # 创建临时分支
    run_git(["add", "."], plugin_dir)
    run_git(["commit", "-m", "feat: final precise clean push of java-plugins"], plugin_dir)
    
    # 4. 强制覆盖远程 main
    print("\n--- 正在强制覆盖远程 main 分支 ---")
    res_main = run_git(["push", "origin", "main_temp:main", "--force"], plugin_dir)
    if res_main.returncode == 0:
        print("成功覆盖远程 main！")
    else:
        print(f"覆盖 main 失败: {res_main.stderr}")

    # 5. 强制覆盖远程 master (防止残留)
    print("\n--- 正在强制覆盖远程 master 分支 ---")
    res_master = run_git(["push", "origin", "main_temp:master", "--force"], plugin_dir)
    if res_master.returncode == 0:
        print("成功覆盖远程 master！")
    else:
        print(f"覆盖 master 失败: {res_master.stderr}")

    print("\n--- 清理完成 ---")

if __name__ == "__main__":
    main()
