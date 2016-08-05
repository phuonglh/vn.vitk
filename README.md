# Vitk -- A Vietnamese Text Processing Toolkit #
---

This is the third release of a Vietnamese text processing toolkit,
which is called "Vitk", developed by [Phuong
LE-HONG](http://mim.hus.vnu.edu.vn/phuonglh) 
 at College of Science, Vietnam National University in Hanoi.

There are some toolkits for Vietnamese text processing which are
already published. However, most of them are not readily scalable for
large data processing. This toolkit aims at the ability of processing
big text data. For this reason, it uses Apache Spark as its core
platform. Apache Spark is a fast and general engine for large
scale data processing. Therefore, Vitk is a fast cluster computing
toolkit.

Despite of its name, this toolkit supports processing in various natural 
languages providing that suitable underlying models or linguistic resources 
are available for the different languages. The toolkit is packaged with models 
and resources for processing Vietnamese. The users can build models for 
other languages using the underlying tools.    


Some examples: 
* The word segmentation tool of Vitk can
tokenize a text of two million Vietnamese syllables in 20 seconds
on a cluster of three computers (24 cores, 24 GB RAM), giving an
accuracy of about 97%.
* The part-of-speech tagger of Vitk can tag about 1,105,000 tokens per second, 
on a single machine, giving an accuracy of about 95% on the Vietnamese treebank.
* The dependency parser of Vitk parses 12,543 sentences (204,586 tokens) of the 
English universal dependency treebank ([English UDT](http://universaldependencies.org/#en))  in less 
than 20 seconds, giving an accuracy of 68.28% (UAS) or 66.30% (LAS).


## Tools ##

Currently, Vitk consists of three fundamental tools for text processing:

* Word segmentation
* Part-of-speech tagging
* Dependency parsing 

The word segmentation tool is specific to the Vietnamese language. The
other tools are general and can be trained to parse any language.
We are working to develop and integrate more fundamental tools to Vitk such as
named entity recognition, constituency parsing, opinion mining, etc.

## Setup and Compilation ##

* Prerequisites: A Java Development Kit (JDK), version 7.0 or
  later [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
	Apache Maven version 3.0 or later [Maven](http://maven.apache.org/). Make
  sure that two following commands work perfectly in your shell
  (console window).

	`java -version`
	
	`mvn -version`

* Download a prebuilt version of [Apache Spark](https://spark.apache.org/).
	Vitk uses Spark version 1.6.x. Unpack the compressed file to a directory,
	for example `~/spark` where `~` is your home directory.

* Download Vitk, either a binary archive or its source code. The
  repository URL of the project is [Vitk](https://github.com/phuonglh/vn.vitk.git).
  The source code version is preferable. It is easy to compile and
  package Vitk: go to the top-level directory of Vitk and invoke the
  following command at a shell window:

	`mvn compile package`

	Apache Maven will automatically resolve and download dependency
	libraries required by Vitk. Once the process finishes, you should
	have a binary jar file `vn.vitk-3.0.jar` in the sub-directory
	`target`. 


## Running ##

### Data Files ###

Data files used by Vitk are specified in sub-directories of the directory `dat`, 
corresponding to its integrated tools. 

* The data used by word segmentation are in the sub-directory 
`dat/tok`.
* The data used by part-of-speech tagging are in the sub-directory 
`dat/tag`.
* The data used by dependency parsing are in the sub-directory 
`dat/dep`.

These folders can contain data specific to a natural language in
use. Each language is specified further by a sub-directory whose name
is an abbreviation of the language name, for example `vi` for
Vietnamese, `en` for English, `fr` for French, etc.

Vitk can run as an application on a stand-alone cluster mode  or on a
real cluster. If it is run on a cluster, it is required that
all machines in the cluster are able to access the same data files,
which are normally located in a shared directory readable by all the
machines.

If you use a Unix-like operating system (Unix/Linux/MacOS), it is easy to share or
"export" a directory via a network file system
([NFS](https://en.wikipedia.org/wiki/Network_File_System)). By
default, Vitk 
searches for data files in the directory `/export/dat/`. Therefore,
you need to copy the sub-directories `dat/*` into that directory, so
that you have some folders as follows: 

* `/export/dat/tok`
* `/export/dat/tok/whitespace.model`
* `/export/dat/tag/vi/cmm`
* `/export/dat/dep/vi/mlp`

If you run Vitk on a stand-alone cluster mode, it is sufficient to
create the data folders specified above on your single machine. The
NFS stuffs can be ignored. 

### Usage ###

Vitk is an Apache Spark application, you run it by submitting the 
main JAR file `vn.vitk-3.0.jar` to Apache Spark. The main class of the
toolkit is `vn.vitk.Vitk` which selects the desired tool by following
arguments provided by the user.  

The general arguments of Vitk are as follows:

* `-m <master-url>`: the master URL for the cluster, for example
  `spark://192.168.1.1:7077`. If you do not have a cluster, you can
  ignore this parameter. In this case, Vitk uses the stand-alone
  cluster mode, which is defined by `local[*]`, that is, it uses all
  the CPU cores of your single machine for parallel processing.

* `-t <tool>`: the tool to run, where `tool` is an abbreviation 
   of the tool: `tok` for word segmentation (or tokenization); `tag` for 
   part-of-speech tagging, `dep` for dependency parsing. If this argument is not 
   specified, the default `tok` tool is used.
   
* `-l <language>`: the natural language to process, where `language` is an abbreviation 
   of language name which is either `vi` (Vietnamese) or `en` (English). If this 
   argument is not specified, the default language is Vietnamese.     
  
* `-v`: this parameter does not require argument. If it is used, Vitk
   runs in verbose mode, in which some intermediate information
   will be printed out during the processing. This is useful for debugging.

Note that if you are processing very large texts, for a better performance, 
you should consider to set appropriate options of the `spark-submit`
command, in particular, `--executor-memory` and `--driver-memory`. See
more  on [submitting Apache Spark
applications](http://spark.apache.org/docs/latest/submitting-applications.html). 

In addition to the general arguments above, a specific tool of Vitk
requires its own arguments. The usage of each tool is described in
their corresponding page as follows:

1. [How to run word segmentation](WS.md)
2. [How to run part-of-speech tagging](POS.md)
3. [How to run dependency parsing](DEP.md)

You can also import the source code of Vitk to your favorite IDE
(Eclipse, Netbeans, etc), compile and run from source, for example,
launch the class `vn.vitk.tok.Tokenizer` for word segmentation,
providing appropriate arguments as described above.

## Documentation ##

The algorithms used by the tools of Vitk can be found in some related
scientific publications. However, some of the main methods implemented
in Vitk have been, and will be described in a more accessible way by
blog posts. For example, the word segmentation method is described in: 
* [Vietnamese word segmentation - Part I](http://tech.fpt.com.vn/en/expert-opinion/vietnamese-word-segmentation-part-i-nd498043.html)
* [Vietnamese word segmentation - Part II](http://tech.fpt.com.vn/en/expert-opinion/vietnamese-word-segmentation-part-ii-nd498054.html)

## Contribution Guidelines ##

* Writing tests
* Code review
* Contributions

## Contact ##

Any bug reports, suggestions and collaborations are welcome. I am
reachable at: 

* LE-HONG Phuong, http://mim.hus.vnu.edu.vn/phuonglh
* College of Science, Vietnam National University in Hanoi 
