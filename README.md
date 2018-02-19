# Halite I Bot
My team's implementation of a bot for Halite I 2016 contest.
The intuition and game strategy is easily understandable from code comments.
Finished 15th place on local faculty competition.

## How to Test
The project contains a Makefile with the following rules:
- make: compile everything
- make clean: clean compiled files
- make rmsim: remove *.hlt* visualization files

The running script is **runGame** (currently configured with seed 2017 on a 50x50 map versus RandomBot):
```sh
$ ./runGame.sh
```

Upload the obtained visualization file here:
https://2016.halite.io/local_visualizer.php

## Documentation
https://2016.halite.io/
