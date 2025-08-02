import matplotlib.pyplot as plt
import numpy as np
import os
import warnings
warnings.filterwarnings("ignore")

def plot_ratio():
    nums = range(20, 81, 10)  # 20, 30,...,80
    data_to_plot = []

    for num in nums:
        filename = f'result/deviation_analysis/avg_ratio_{num}.txt'
        if not os.path.exists(filename):
            data_to_plot.append([])
            continue
        
        with open(filename, 'r') as f:
            content = f.read().strip()
            # 清理数据：去除多余的逗号，按逗号或空格分割
            numbers = []
            for item in content.replace('\n', '').split(','):
                item = item.strip()  # 去除前后空格
                if item:  # 确保不是空字符串
                    try:
                        numbers.append(float(item))
                    except ValueError:
                        print(f"Encountered value in file {filename} that cannot be converted: '{item}'. Skip!")
        
        if not numbers:
            print(f"There are invalid data in {filename}. Skip!")
            data_to_plot.append([])
            continue
        transformed = [x for x in numbers if x <= 1.0 and x > 0.0]
        filtered = [100*(1.0 - x) for x in transformed]
        data_to_plot.append(filtered)

    # 创建箱线图
    plt.figure(figsize=(12, 6))
    plt.boxplot(data_to_plot, labels=[str(num) for num in nums], showfliers=False)

    plt.yticks([0, 5, 10, 15])
    plt.ylim(-2, 17)

    plt.xlabel('Portion of probabilistic structures')
    plt.ylabel('Deviation (%)')
    plt.grid(True, linestyle='--', alpha=0.7)
    res_file='result/deviation_analysis_ratio.pdf'
    plt.savefig(res_file, format='pdf')
    print(f'The figure describing the deviation in percentage between the proposed analysis and Ueter2021 under varied psr is generated in {res_file}')


def plot_para():
    nums = range(3, 10, 1)
    data_to_plot = []

    for num in nums:
        filename = f'result/deviation_analysis/avg_para_{num}.txt'
        if not os.path.exists(filename):
            data_to_plot.append([])
            continue

        numbers = []

        with open(filename, 'r') as f:
            content = f.read().strip()    
            for item in content.replace('\n', '').split(','):
                item = item.strip()
                if item:
                    try:
                        numbers.append(float(item))
                    except ValueError:
                        print(f"Encountered value in file {filename} that cannot be converted: '{item}'. Skip!")
        
        if not numbers:
            print(f"There are invalid data in {filename}. Skip!")
            data_to_plot.append([])
            continue
        transformed = [x for x in numbers if x <= 1.0 and x > 0.0]
        filtered = [100*(1.0 - x) for x in transformed]
        data_to_plot.append(filtered)

    # 创建箱线图
    plt.figure(figsize=(12, 6))
    plt.boxplot(data_to_plot, labels=[str(num) for num in nums], showfliers=False)

    plt.yticks([0, 5, 10, 15])
    plt.ylim(-2, 17)

    plt.xlabel('Number of nodes per layer')
    plt.ylabel('Deviation')
    plt.grid(True, linestyle='--', alpha=0.7)
    res_file = 'result/deviation_analysis_para.pdf'
    plt.savefig(res_file, format='pdf')
    print(f'The figure describing the deviation in percentage between the proposed analysis and Ueter2021 under varied para is generated in {res_file}')


def plot_cond():
    nums = range(2, 8, 1)
    data_to_plot = []

    for num in nums:
        filename = f'result/deviation_analysis/avg_cond_{num}.txt'
        if not os.path.exists(filename):
            data_to_plot.append([])
            continue
        
        with open(filename, 'r') as f:
            content = f.read().strip()
            numbers = []
            for item in content.replace('\n', '').split(','):
                item = item.strip()
                if item:
                    try:
                        numbers.append(float(item))
                    except ValueError:
                        print(f"Encountered value in file {filename} that cannot be converted: '{item}'. Skip!")
        
        if not numbers:
            print(f"There are invalid data in {filename}. Skip!")
            data_to_plot.append([])
            continue
        transformed = [x for x in numbers if x <= 1.0 and x > 0.0]
        filtered = [100*(1.0 - x) for x in transformed]
        data_to_plot.append(filtered)

    # 创建箱线图
    plt.figure(figsize=(12, 6))
    plt.boxplot(data_to_plot, labels=[str(num) for num in nums], showfliers=False)

    plt.yticks([0, 5, 10, 15])
    plt.ylim(-2, 17)

    plt.xlabel('Number of probabilistic structures')
    plt.ylabel('Deviation')
    plt.grid(True, linestyle='--', alpha=0.7)
    res_file = 'result/deviation_analysis_cond.pdf'
    plt.savefig(res_file, format='pdf')
    print(f'The figure describing the deviation in percentage between the proposed analysis and Ueter2021 under varied cond is generated in {res_file}')


if __name__ == '__main__':
    plot_ratio()
    plot_para()
    plot_cond()
    