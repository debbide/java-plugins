import os
import subprocess
import shutil

def run_git(args, cwd):
    return subprocess.run(["git"] + args, cwd=cwd, capture_output=True, text=True, encoding='utf-8', errors='ignore')

def main():
    plugin_dir = r"e:\ck\docker\bungeecord-main\java-plugins-main"
    outer_dir = r"e:\ck\docker\bungeecord-main"
    
    print(f"--- 正在检查目录: {plugin_dir} ---")
    
    # 1. 检查当前所在的 Git 根目录
    result = run_git(["rev-parse", "--show-toplevel"], plugin_dir)
    git_root = result.stdout.strip()
    print(f"Git 根目录: {git_root}")
    
    # 2. 检查远程仓库
    result = run_git(["remote", "-v"], plugin_dir)
    print(f"远程仓库:\n{result.stdout}")
    
    # 3. 列出当前跟踪的前 10 个文件
    result = run_git(["ls-files"], plugin_dir)
    files = result.stdout.splitlines()
    print(f"跟踪文件总数: {len(files)}")
    print("前 10 个跟踪文件:")
    for f in files[:10]:
        print(f"  - {f}")

    # 4. 判定是否存在越界文件
    is_messy = any(not f.startswith("java-plugins-main") and not os.path.exists(os.path.join(plugin_dir, f)) for f in files)
    
    if git_root.lower() == outer_dir.lower() or len(files) > 100:
        print("\n[警告] 检测到仓库状态混乱：插件目录正在使用外部目录的 .git 或包含了过多无关文件。")
    else:
        print("\n[正常] 仓库状态看起来是局部的。")

if __name__ == "__main__":
    main()
