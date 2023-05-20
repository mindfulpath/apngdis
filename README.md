# apngdis
A command-line APNG disassembler

An imitation of Max Stepin’s [apngdis](https://sourceforge.net/projects/apngdis/), same arguments, many of the same messages. Basically (e.g., using GraalVM to make a native image, or `alias apngdis=java -jar /path/to/apngdis.jar`):

```apngdis anim.png [name]```

where `anim.png` is the APNG file and `name` is the prefix for the animation frames.

This one is written in pure Java, using Alex Dupre’s [fork of PNGJ](https://github.com/alexdupre/pngj) to run in parallel on modern computers, and to copy the chunks that are needed for accurate color.

## License
Apache 2.0
