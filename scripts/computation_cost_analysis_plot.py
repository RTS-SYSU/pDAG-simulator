# =================================================================================== #
# ===========================         fig5            =============================== #
# =================================================================================== #

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
import matplotlib.ticker as ticker
from matplotlib.ticker import MaxNLocator
from matplotlib.font_manager import FontProperties
import os

def read_variables_and_values(filename, bound=4):
    variables = []
    values = []

    if not os.path.exists(filename):
        variables = ["Ueter", "Our"]
        values = [[np.nan for _ in range(500)] for _ in range(2)]
        return variables, values
    
    with open(filename, 'r') as file:
        lines = file.readlines()
        for i in range(bound):
            line = lines[i].strip()
            if i % 2 == 0:
                variables.append(line[:-1])
            else:
                # ======================================================================= #
                if variables[-1] != "Our" and ("all_8" in filename or "all_9" in filename or "all_10" in filename):
                    values.append([np.nan for _ in range(500)])
                    continue
                # ======================================================================= #

                # ======================================================================= #
                if variables[-1] != "Our" and ("all_cond_8" in filename or "all_cond_9" in filename or "all_cond_10" in filename):
                    values.append([np.nan for _ in range(500)])
                    continue
                # ======================================================================= #
                values.append([float(x.strip()) for x in line.split(',')[:500] if x != ''])
                
    
    return variables, values

def read_file_to_df(variables, values, index):
    return pd.DataFrame({
        'Number of Conditional Structures':[index] * len(values[0]),
        "Ueter": values[0],
        variables[1]: values[1],
    })

if __name__ == '__main__':
    df_combined = pd.DataFrame()

    # ========================all========================= # 
    for i in range(2, 11):
        File_name = "result/computation_cost_analysis/time_all_cond_" + str(i) + ".txt"
        variables, values = read_variables_and_values(File_name)
        df = read_file_to_df(variables, values, i)
        df_combined = pd.concat([df_combined, df], ignore_index=True)
    # ===================================================== # 

    # ========================parallelism========================== # 
    # for i in range(4, 11):
    #     File_name = "time_all_para_" + str(i) + ".txt"
    #     variables, values = read_variables_and_values(File_name)
    #     df = read_file_to_df(variables, values, i)
    #     df_combined = pd.concat([df_combined, df], ignore_index=True)
    # ===================================================== # 

    # ========================ratio========================== # 
    # for i in range(20, 81, 10):
    #     File_name = "time_all_ratio_" + str(i) + ".txt"
    #     variables, values = read_variables_and_values(File_name)
    #     df = read_file_to_df(variables, values, i)
    #     df_combined = pd.concat([df_combined, df], ignore_index=True)
    # ===================================================== #

    df_combined.to_excel('Combined_output.xlsx', index=False, header=True)


    plt.rcParams.update({
        # 'font.family': 'Times New Roman',
        'font.size': 14,
        'axes.labelsize': 16,
        'axes.titlesize': 18,
        'xtick.labelsize': 14,
        'ytick.labelsize': 14,
        'legend.fontsize': 22,
        'figure.figsize': (8, 4),
        'axes.linewidth': 1.2,
    })

    mean_values = df_combined.groupby('Number of Conditional Structures').mean()
    std_values = df_combined.groupby('Number of Conditional Structures').std()
    n_values = df_combined.groupby('Number of Conditional Structures').count()
    se_values = std_values / np.sqrt(n_values)

    sns.set_palette("deep")

    plt.figure()

    markers = ['o', 'x', '^', 'D']
    linestyles = ['-', '-', '--', '--']
    markersize=12
    errorbar_elinewidth=1

    for idx, variable in enumerate(df_combined.columns[1:]):
        line_color = 'black'
        # print(mean_values.index)
        # print(mean_values[variable])
        plt.plot(mean_values.index, mean_values[variable],
                marker=markers[idx % len(markers)], markerfacecolor='none',
                markeredgewidth=1.3,
                linestyle=linestyles[idx % len(linestyles)],
                color=line_color,
                linewidth=2,
                markersize=markersize,
                #  label=variable,
                label="Enumeration" if idx == 0 else "Proposed",
                ) 

    major_tick_len = 0
    minor_tick_len = 0


    plt.ylabel('Comput. Cost (ms)', fontsize=24)

    plt.yscale('log')
    ax = plt.gca()
    # ax.yaxis.set_minor_locator(ticker.LogLocator(base=6))

    plt.gca().tick_params(
            axis='y',
            direction='in',
            length=major_tick_len
    )       
    plt.gca().tick_params(
            axis='y',
            which='minor',
            direction='in',
            length=minor_tick_len
    )      

    y_ticks = [0.01,1, 100, 10000, 1000000] 
    plt.ylim(0.01, 1000000)

    plt.gca().yaxis.set_major_locator(ticker.MaxNLocator(nbins=5)) 

    plt.yticks(y_ticks, fontsize=25)

    plt.tick_params(axis='x',
                    pad=6,
                    labelsize=23)
    plt.tick_params(axis='y',
                    labelsize=22)
    # ================================================



    plt.gca().xaxis.set_major_locator(MaxNLocator(integer=True))
    plt.xticks(fontsize=25)
    plt.xlim(2, 10)  

    ax = plt.gca()
    ax.xaxis.set_minor_locator(ticker.MultipleLocator(0.5))

    plt.gca().tick_params(
            axis='x',
            direction='in',
            length=major_tick_len
    )      
    plt.gca().tick_params(
            axis='x',
            which='minor',
            direction='in',
            length=minor_tick_len
    )      
    # ================================================

    ax = plt.gca()
    for spine in ax.spines.values():
        spine.set_linewidth(2)
    # ======================================

    # legend_font = FontProperties(family='Times New Roman', size=24)
    legend_font = FontProperties(size=24)
    # legend = plt.legend(loc='upper right', prop=legend_font, handlelength=1.2)
    legend = plt.legend(
        loc='upper right',
        bbox_to_anchor=(1.01, 0.808),
        prop=legend_font, 
        handlelength=1.6,       
        handletextpad=0.2,      
        borderpad=0.2,          
        fancybox=False,         
        )  
    frame = legend.get_frame()
    frame.set_facecolor('white')
    frame.set_alpha(0.8) 
    frame.set_edgecolor('black')  # 
    frame.set_linewidth(1.5)  # 

    # =================================================

    plt.grid(linestyle='--', alpha=0.7)

    plt.tight_layout()
    res_file = 'result/computation_cost_analysis.pdf'
    plt.savefig(res_file, format='pdf')
    # plt.savefig('exp2.svg', dpi=300, format='svg')

    print(f'The figure comparing the computation cost is generated in {res_file}')
