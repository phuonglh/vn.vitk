## Vitk -- Dependency Parsing ##
---

The dependency parser of Vitk implements a transition-based dependency parsing 
algorithm. At its core, this algorithm requires a classifier to 
classify each parsing configuration into a transition. Vitk uses a multi-layer 
perceptron (MLP) model (or an artificial neural network) as its classifier. This is a 
simple network in that it either contains no more than one hidden layer. 
  

### Modes ##

The parser has three modes: `parse`, `eval`, and `train`. In each run,
it executes in one of these modes.

1. In the `parse` mode, the parser loads a pre-trained MLP, reads
  and parses an input text file and writes the result to the console or
  to an output file if specified.
  * A pre-trained MLP is located in a data folder of the parser. Vitk
    comes with a pre-trained 2-layer MLP for Vietnamese, which is located in the
    directory `dat/dep/vi/mlp`. This MLP is trained by using 2,700
    parsed sentences of a Vietnamese Treebank.
  * The input text file is a plain text in UTF-8 encoding containing part-of-speech 
  tagged sentence, each on a line, in a simple format as follows:
  `Đất/N nghèo/A trở_mình/V ./.` That is, each word is paired with its
  correct part-of-speech tag, separated by the slash character.  
2. In the `eval` mode, the parser loads a pre-trained MLP, reads
  and evaluates the accuracy of the parser on an input text file, and
  reports the performance of the parser to the console.
  * A pre-trained MLP is similar to that in the `parse` mode
  described above.
  * However, the input text file now contains manually parsed
  sentences in the CoNLL-X format (or in the CoNLL-U format for English). An example
  of this format is given at the end of this page.  
  * The result of an evaluation is the accuracy of the parsing. The
  parser reports the unlabelled attachment score (UAS) and labelled attachment score (LAS),
  both at the token level and sentence level. All the results are
  printed to the console window.
3. In the `train` mode, the parser reads manually parsed sentences
  from an input text file, trains a MLP and saves the model to
  an output file. There are some options for used in training which
  will be described in detail in the next section.
  * The input text file contains correct parsed sentences in the
    CoNLL-X or CoNLL-U format as described in the `eval` mode above. The result of
  training is a MLP, which is then saved to an output
  file. 
  * Pre-trained parsing models provided by Vitk are also created 
  by running the parser in this mode.

### Arguments ###

The parser of Vitk has following arguments:

* `-a <mode>`: the mode, or action of the parser, which is either
  `parse`, `eval`, or `train`. If this argument is not specified, the
  default mode of the parser is `parse`.
* `-i <input-file>`: the name of an input file to be used. This
   should be a text file in UTF-8 encoding. If the parser is in the
   `parse` mode, it will read and parse every lines of this file. If it is
   in the `eval` or `train` mode, it will read in the parsed sentences of this 
   file, parse them and evaluate the accuracy or train a MLP model.  
* `-o <output-file>`: the name of an output file containing the
   parsing results (in the `parse` mode). The result of a parse is a list of flattened 
   relations in the form of `relation(sourceToken, targetToken)`. 
   Since by default, Vitk uses Hadoop file system to save results, the output file is actually a
   directory. It contains text files in JSON format, and will be created by
   Vitk. Note that this directory must not already exist, otherwise an
   error will be thrown because Vitk cannot overwrite an existing
   directory. If this parameter is not specified, the result is
   printed to the console window.  
   
* `-mlp <mlp-file>`: the name of a MLP file containing a classification 
   model. In the `parse` mode, this is a pre-trained MLP, while in the
   `train` mode, this is the resulting MLP of the training. If this
   argument is not specified, the parser will use the default
   file named `dat/dep/vi/mlp`. 
* `-dim <dimension>`: this argument is only required in the `train` mode
  to specify the number of features (or the domain dimension) of the
  resulting MLP. The dimension is a positive integer and depends on
  the size of the data. Normally, the larger the training data is, the
  greater the dimension that should be used. Vitk implements the
  [feature hashing](https://en.wikipedia.org/wiki/Feature_hashing) 
  trick, which is a fast and space-efficient way of vectorizing
  features. As an example, we set this argument as 90,000 when
  training a 2-layer MLP on the training set of the English UDT. 
* `-hid <numHiddenUnits>`: this argument is only used in the `train` mode to
  specify the number of norons (or units) in the hidden layer of the neural network.
  This is a positive integer. The default value of `numHiddenUnits` is zero, that is 
  there is no hidden layer, the network contains only an input layer of `dimension` 
  units, and an output layer of `K` units where `K` is the number of different classes 
  that the network learns to classify. 
  
### Running ###

Suppose that Apache Spark has been installed in `~/spark`, Vitk has
been installed in `~/vitk`, data files required by this tool have been
copied to `/export/dat/dep`. To launch Vitk, open a console, enter the
folder `~/spark` and invoke an appropriate command. For example:


* To parse an input file and write the result to an output file, using
  the default pre-trained MLP:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t dep -a parse -i
  <input-file> -o <output-file>`

Because the default mode of the parser is `parse`, we can drop the argument 
`-a parse`in the command above.

There is not any `-m` argument in the command above, therefore, Vitk
runs in the stand-alone cluster mode which uses a single local machine
and all CPU cores.

* To parse an input file, write the result to an output file, using a
   specified pre-trained MLP:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t dep -i
  <input-file> -o <output-file> -mlp <mlp-file>` 

* To evaluate the accuracy on a gold corpus, using the default
   pre-trained MLP:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t dep -a eval -i
  <input-file>` 

* To train a parser on a gold Vietnamese corpus, producing a 2-layer MLP:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t dep -a train -dim
  50000 -i <input-file>` 

* To train a parser on a gold Vietnamese corpus, producing a 3-layer MLP with 
a hidden layer of 16 units:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t dep -a train -dim
  50000 -hid 16 -i <input-file>` 

The resulting MLP is then saved to the
default directory `/export/dat/dep/vi/mlp`. If you want to save the result
to a specific directory, append the `-mlp` argument, for example:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t dep -a train -dim
  50000 -hid 16 -i <input-file> -mlp /tmp/myMLP` 

* To train a parser on a gold English UDT corpus, producing a 2-layer MLP in the 
 verbose mode:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -l en -t dep -a train -dim
  50000 -i <input-file> -v` 

The resulting MLP is then saved to the default directory `/export/dat/dep/en/mlp`. 

If you wish to run Vitk on a Spark cluster, all you need to do is to
specify the master URL of the cluster, such as: 

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t dep -a parse -i
  <input-file> -o <output-file> -m <master-url>` 

For your convenience, Vitk includes a sample file containing 270 manually 
parsed sentences which are extracted from the Vietnamese treebank.

### Experimental Results ###

On the English UDT: 

* 12,543 parsed sentences for training, 2,077 parsed 
  sentences for testing 
* Standard feature templates (token, part-of-speech tag, dependency type)  
* Training parameters: dimension=90,000  
* Training accuracy: 68.28% (UAS), 66.32% (LAS)
* Test accuracy: 64.18% (UAS), 60.96% (LAS) 
* Parsing speed on a single machine: tag 12,543 sentences (204,586 tokens) in 20 seconds. 

On a Vietnamese dependency treebank:
* 2,430 parsed sentences for training, 270 sentences for testing
* Standard feature templates (token, part-of-speech tag, dependency type)
* Training parameters: dimension=30,000
* Training accuracy: 90.44% (UAS), 90.23% (LAS) 
* Test accuracy: 67.08% (UAS), 62.78% (LAS)
 
 

### Dependency Formats ##

* [CoNLL-U format](http://universaldependencies.org/)
* [CoNLL-X format](http://ilk.uvt.nl/conll/) 

### References ###
* [An efficient algorithm for projective dependency parsing](http://stp.lingfil.uu.se/~nivre/docs/iwpt03.pdf), 
 Joakim Nivre, Proceedings of IWPT, 2003.
* [Algorithms for deterministic incremental dependency parsing](http://www.mitpressjournals.org/doi/pdfplus/10.1162/coli.07-056-R1-07-027), Joakim Nivre, Computational Linguistics, 2008. 

---
[Back to Vitk](https://github.com/phuonglh/vn.vitk)
