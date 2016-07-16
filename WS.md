## Vitk -- Word Segmentation ##
---

Word segmentation is the default tool of Vitk. It accepts a source
text and segments the text into tokens. The source text may come
either from an input text file or from an URL, as specified in the
usage below.

### Arguments ###

The word segmentation tool of Vitk has following arguments:

* `-i <input-file>`: the name of an input file to be segmented. This
   should be a text file in UTF-8 encoding. Vitk will read and
   tokenize every lines of this file.
* `-u <url>`: an Internet URL containing Vietnamese text to be
   segmented. This is normally an URL to an electronic newspaper, for
   example "http://vneconomy.vn/thoi-su/bo-cong-thuong-len-tieng-ve-sieu-du-an-tren-song-hong-20160507072218349.htm".
	 If this parameter is used, Vitk will extract the main text content
   of the article and tokenize it, line by line. Note that either
   parameter `-i` or `-u` should be used.
* `-o <output-file>`: the name of an output file containing the
   segmentation results. Since by default, Vitk uses Hadoop file
   system to save results, this is actually a directory containing
   output text files and will be created by Vitk. Note that this 
   directory must not already exist, otherwise an error will be thrown
   because Vitk cannot overwrite an existing directory. If this
   parameter is not specified, the result is printed to the console window.
* `-s`: this parameter does not require argument. If it is used, Vitk
   uses a logistic regression model to disambiguate the space
   character when segmenting Vietnamese phrases instead of using a
   phrase graph as default. The model is
   pre-trained and loaded from the data directory `whitespace.model`.
   However, the result is often worse than the default. Thus, you should
   consider to use this option only as an extra experimentation.
 
### Running ###

Suppose that Apache Spark has been installed in `~/spark`, Vitk has
been installed in `~/vitk`, data files required by this tool have been
copied to `/export/dat/tok`. To launch Vitk, open a console, enter the
folder `~/spark` and invoke an appropriate command. For example:

*	`./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -u <url>` 
* `./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -i <input-file>`
* `./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -i <input-file> -o
  <output-file>`
* `./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -m <master-url> -u
  <url>`
* `./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -m <master-url> -i
  <input-file> -o <output-file>` 
* `./bin/spark-submit ~/vitk/target/vn.vitk-3.0.jar -m <master-url> -i
  <input-file> -o <output-file> -v` 

### References ###
* [A hybrid approach to word segmentation of Vietnamese texts](http://mim.hus.vnu.edu.vn/phuonglh/node/33), 
Phuong Le-Hong et al., Springer LNCS 5196, 2008.


---
[Back to Vitk](https://github.com/phuonglh/vn.vitk)
