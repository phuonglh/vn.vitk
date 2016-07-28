## Vitk -- Part-of-Speech Tagging ##
---

The part-of-speech tagger of Vitk implements a conditional Markov
model (CMM), a common probabilistic model for sequence labelling. In
essence, CMM is a discriminative model which models the conditional
probability distribution `P(tag sequence|word sequence)`. This
probability is decomposed into a chain of local probability
distributions `P(tag|word)` by using the Markov property. Each local
probability distribution is a log-linear model (also called a maximum
entropy model). 

### Modes ##

The tagger has three modes: `tag`, `eval`, and `train`. In each run,
it executes in one of these modes.

1. In the `tag` mode, the tagger loads a pre-trained CMM, reads
  and tags an input text file and writes the result to the console or
  to an output file if specified.
  * A pre-trained CMM is located in a data folder of the tagger. Vitk
	comes with a pre-trained CMM for Vietnamese, which is located in the
	directory `dat/tag/vi/cmm`. This CMM is trained by using 10,165
	tagged sentences of a Vietnamese Treebank.
  * The input text file is a plain text in UTF-8 encoding which has
  been word segmented, and it will be tagged line by line. 
2. In the `eval` mode, the tagger loads a pre-trained CMM, reads
  and evaluates the accuracy of the tagger on an input text file, and
  reports the performance of the tagger to the console.
  * A pre-trained CMM is similar to that in the `tag` mode
  described above.
  * However, the input text file now contains manually tagged
  sentences, each sentence on a line, in a simple format as follows:
  `Đất/N nghèo/A trở_mình/V ./.` That is, each word is paired with its
  correct tag, separated by the slash character.
  * The result of an evaluation is the accuracy of the tagging. The
  tagger reports token accuracy, that is the percentage of tokens
  which have been correctly tagged. If the toolkit is run with verbose
  option, the tagging speed is also reported. All the results are
  printed to the console window.
3. In the `train` mode, the tagger reads manually tagged sentences
  from an input text file, trains a CMM and saves the model to
  an output file. There are some options for used in training which
  will be described in detail in the next section.
  * The input text file contains correct tagged sentences in the
    simple format as described in the `eval` mode above. The result of
  training is a CMM, which is then saved to an output
  file. 
  * Pre-trained tagging models provided by Vitk are also created 
  by running the tagger in this mode.

### Arguments ###

The tagger of Vitk has following arguments:

* `-a <mode>`: the mode, or action of the tagger, which is either
  `tag`, `eval`, or `train`. If this argument is not specified, the
  default mode of the tagger is `tag`.
* `-i <input-file>`: the name of an input file to be used. This
   should be a text file in UTF-8 encoding. If the tagger is in the
   `tag` mode, it will read and tag every lines of this file. If it is
   in the `eval` or `train` mode, it will preprocess the file to get
   pairs of word and tag sequences for use in evaluating or training. 
* `-o <output-file>`: the name of an output file containing the
   tagging results (in the `tag` mode). Since by default, Vitk uses
   Hadoop file system to save results, the output file is actually a
   directory. It 
   contains text files in JSON format, and will be created by
   Vitk. Note that this directory must not already exist, otherwise an
   error will be thrown because Vitk cannot overwrite an existing
   directory. If this parameter is not specified, the result is
   printed to the console window. 
* `-cmm <cmm-file>`: the name of a CMM file containing a tagging
   model. In the `tag` mode, this is a pre-trained CMM, while in the
   `train` mode, this is the resulting CMM of the training. If this
   argument is not specified, the tagger will use the default
   file named `dat/tag/vi/cmm`. This contains binary
   files in the `parquet` format of Apache Spark.
* `-dim <dimension>`: this argument is only required in the `train` mode
  to specify the number of features (or the domain dimension) of the
  resulting CMM. The dimension is a positive integer and depends on
  the size of the data. Normally, the larger the training data is, the
  greater the dimension that should be used. Vitk implements the
  [feature hashing](https://en.wikipedia.org/wiki/Feature_hashing) 
  trick, which is a fast and space-efficient way of vectorizing
  features. As an example, we set this argument as 160,000 when
  training a CMM on about 10,000 tagged sentences of the Vietnamese
  treebank.
* `-reg <lambda>`: this argument is only used in the `train` mode to
  specify the L2-regularization term of the objective function to be
  minimized. This is a non-negative real value. The default value of
  `lambda` is zero, that is there is no regularization. Using a small
  `lambda` often helps avoid overfitting and speed up the training. As an
  example, we set `lambda=1e-6` when training a CMM on about 10,000
  tagged sentences of the Vietnamese treebank.

  
### Running ###

Suppose that Apache Spark has been installed in `~/spark`, Vitk has
been installed in `~/vitk`, data files required by this tool have been
copied to `/export/dat/tag`. To launch Vitk, open a console, enter the
folder `~/spark` and invoke an appropriate command. For example:


* To tag an input file and write the result to an output file, using
  the default pre-trained CMM:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t tag -a tag -i
  <input-file> -o <output-file>`

Because the default mode of the tagger is `tag`, we can drop the argument 
`-a tag`in the command above.

There is not any `-m` argument in the command above, therefore, Vitk
runs in the stand-alone cluster mode which uses a single local machine
and all CPU cores.

* To tag an input file, write the result to an output file, using a
   specified pre-trained CMM:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t tag -a tag -i
  <input-file> -o <output-file> -cmm <cmm-file>` 

* To evaluate the accuracy on a gold corpus, using the default
   pre-trained CMM:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t tag -a eval -i
  <input-file>` 

* To train a tagger on a gold corpus:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t tag -a train -dim
  10000 -reg 1e-6 -i <input-file>` 

The resulting CMM has 10 thousand dimensions and is saved to the
default directory `/export/dat/tag/vi/cmm`. If you want to save the result
to a specific directory, append the `-cmm` argument, for example:

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t tag -a train -dim
  10000 -reg 1e-6 -i <input-file> -cmm /tmp/myCMM` 

If you wish to run Vitk on a Spark cluster, all you need to do is to
specify the master URL of the cluster, such as: 

`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -t tag -a tag -i
  <input-file> -o <output-file> -m <master-url>` 

For your convenience, Vitk includes a sample file containing 1,000
tagged sentences which are extracted from the Vietnamese treebank.

### Experimental Results ###

On the Vietnamese treebank: 

* 9,000 tagged sentences for training, 1,165 tagged
  sentences for testing. 
* Training parameters: dimension=160,000; lambda=1e-6. 
* Training accuracy: 99.65%; test accuracy: 95.17%. 
* Tagging speed on a single machine: tag 221,112 tokens in 216 milliseconds, 
  that is, about 1,105,000 tokens per second. 

### Vietnamese Part-Of-Speech Tagset ##

The tagset used by Vitk is that of the Vietnamese treebank. There are
18 different tags:

1.  Np - Proper noun
2.  Nc - Classifier
3.  Nu - Unit noun
4.  N - Common noun
5.  V - Verb
6.  A - Adjective
7.  P - Pronoun
8.  R - Adverb
9.  L - Determiner
10. M - Numeral
11. E - Preposition
12. C - Subordinating conjunction
13. CC - Coordinating conjunction
14. I - Interjection
15. T - Auxiliary, modal words
16. Y - Abbreviation
17. Z - Bound morphemes
18. X - Unknown


### References ###
* [A maximum entropy model for part-of-speech tagging](http://www.aclweb.org/anthology/W/W96/W96-0213.pdf), 
 Adwait Ratnaparkhi, Proceedings of EMNLP, 1996.
* [An empirical study of maximum entropy approach for part-of-speech tagging of Vietnamese texts](http://mim.hus.vnu.edu.vn/phuonglh/node/40), 
Phuong Le-Hong et al., Proceedings of TALN, 2010.

---
[Back to Vitk](https://github.com/phuonglh/vn.vitk)
