import pandas as pd

df = pd.read_csv('../resources/benchmark.csv', delimiter=';')
fig = df.set_index('requests').rename(columns={"blocking": "Blocking Calls", "reactive": "Reactive Calls"}).plot(
    figsize=(7.50, 3.50), xlabel='Total Requests', ylabel='Throughput (Requests per Second)').get_figure()
fig.savefig("../resources/benchmark.svg")
