Scalatron - Learn Scala With Friends
=========

This is the source code for Scalatron, a competitive multi-player programming game in which coders pit bot programs
(written in Scala) against each other. It is an educational resource for groups of programmers or individuals that
want to learn more about the Scala programming language or want to hone their Scala programming skills. 

Follow Scalatron on Twitter at [@scalatron](http://twitter.com/scalatron).


## How to Run

If you want to run Scalatron to play the game or to run a workshop with friends, simply [download the latest version](http://github.com/scalatron/scalatron/downloads) - you do not need the source code. Unzip the downloaded file into a local directory, then look for the Readme.txt file to get started. Have fun!


## How to Build

If you want to get the source code and build Scalatron yourself, [download the sources of the 'master' branch](http://github.com/scalatron/scalatron/zipball/master) and build them with [SBT](http://github.com/harrah/xsbt). Once you have SBT installed, switch to the directory where you downloaded the Scalatron sources (the directory that contains this the `build.sbt` file) and run

    sbt dist

This will generate a new directory called `dist` which contains the same content as the regular distribution. Now `cd` into /dist/bin and run `java -jar Scalatron.jar`. 


## Contributing

[Fork Scalatron here on github](https://github.com/scalatron/scalatron/fork) and send pull requests. 
Bring your own ideas or check out the [list of open issues](https://github.com/scalatron/scalatron/issues?state=open). 
Before embarking on something major, you can [contact the maintainer](mailto:scalatron@hotmail.com) and ask for feedback or tips on where to get started.  


## License

Scalatron is licensed under the Creative Commons Attribution 3.0 Unported License. The documentation, tutorial and source code are intended as a community resource and you can basically use, copy and improve them however you want. Included works are subject to their respective licenses. 
