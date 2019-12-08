# CPatMiner: Graph-based Mining of In-the-Wild, Fine-grained, Semantic Code Change Patterns

## Contributors
* [Danny Dig](http://dig.cs.illinois.edu/)
* [Michael Hilton](http://www.cs.cmu.edu/~mhilton/)
* [Hoan Anh Nguyen](https://sites.google.com/site/nguyenanhhoan/) (Project Lead)
* Son Nguyen
* [Tien N. Nguyen](http://www.utdallas.edu/~tien.n.nguyen/)
* [Hieu Tran](https://utdallas.edu/~trunghieu.tran/)

## Publications
* [Graph-based Mining of In-the-Wild, Fine-grained, Semantic Code Change Patterns](https://2019.icse-conferences.org/event/icse-2019-technical-papers-graph-based-mining-of-in-the-wild-fine-grained-semantic-code-change-patterns) (The 41st ACM/IEEE International Conference on Software Engineering
(ICSE 2019) - Technical Track)

## Packages
AtomicASTChangeMining: extracts change graphs from commits.

SemanticChangeGraphMiner: mines change templates from change graphs.

## Extracting change graphs from commits

main class: https://github.com/nguyenhoan/CPatMiner/blob/master/AtomicASTChangeMining/src/main/MainChangeAnalyzer.java

arguments:

-i input_repos_root_path: each sub folder is a git repo

-o output_path: where the graphs are stored

## Mining change templates from change graphs

main class: https://github.com/nguyenhoan/CPatMiner/blob/master/SemanticChangeGraphMiner/src/main/MineChangePatterns.java

reposPath = input_repos_root_path: a sub folder is a git repo

changesPath = change_graph_path: output_path of the extraction step

file list.csv under reposPath: a text file containing the names of repos to be processed, one repo name on each line. ls reposPath > list.csv to create this file if you want to process all repos

output: in a directory patterns/input_repos_root_path-hybrid under the working directory

note: the directory https://github.com/nguyenhoan/CPatMiner/tree/master/SemanticChangeGraphMiner/src/resources has to be in a directory named src under the working directory.


## License
All software provided in this repository is subject to the [Apache License Version 2.0](LICENSE).
