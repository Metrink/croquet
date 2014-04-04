Croquet Documentation
=====================

This sphinx configuration was cloned from [Sphinx Markdown Sample](https://github.com/mctenshi/sphinx-markdown-sample).

Build
-----

First install `pandoc` using apt. Then, from the croquet project root directory, build the documentation with maven:

```
$ mvn site
```

This will generate `target/site/index.html`.


Technical Note
--------------

The maven sphinx plugin uses jython 1.5. The markdown library was written for 1.6. It had to be forked to work in this environment.