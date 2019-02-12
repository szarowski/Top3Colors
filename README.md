# Top Three Colors

The application searches three most often used colors in the images provided in the input file lines in the form of  links.
The results are stored in the output file in format `link,color,color,color`.

The last line printed before exiting shows the time taken for processing.    

## Running the application

The minimum requirements for Java is Java 11 due to the new efficient asynchronous non-blocking client used in the application.
 
The project is build in Gradle, use:
```
./gradlew clean build
java -jar build/libs/Top3Colors-1.0-SNAPSHOT.jar input.txt output.csv
```
Running the application with no parameter shows a simple help with parameters description:
```
java -jar build/libs/Top3Colors-1.0-SNAPSHOT.jar
```

Minimum recommended heap memory is 768 MB:
```
java -Xmx768M -jar build/libs/Top3Colors-1.0-SNAPSHOT.jar input.txt output.csv
```

Running with 2048 MB heap memory on 2 cores:
```
java -Xmx2048M -jar build/libs/Top3Colors-1.0-SNAPSHOT.jar input.txt output.csv 2
```
 
## Design notes

The problem can be decomposed into a chain of several tasks:
1. Process a file with a provided file name with links as an unbounded stream
2. Select a link from the file
3. Connect to a web site and download the content of the image 
4. Process the image (select 3 most often used colors)
5. Print out the link together with the colors in a hexadecimal form of RGB, comma separated

The application not always works in a single-threaded mode on 512 MB of heap memory for the links to images provided.
Therefore suggested heap memory size is estimated to approx. at least 768 MB
or to use another garbage collector in combination with -Xmx512M setting.
The memory utilization is stable as all the containers and streams are flushed or closed after they are used.
The set of links provided in the input file can be much longer than the default 1000 links.
 
The architecture tries to maximize utilization of the hardware CPU cores by running
the chain in parallel. However, the concurrency level must be adjusted according to the used memory.
Assuming the worst case scenario, a linear memory increase is expected.
The application automatically selects the number of cores by formula:
```
// estimate number of cores based on Xmx used (2 << 28 corresponds to 512 MB for a single thread execution)
int cores = (int) (Runtime.getRuntime().maxMemory() / (2 << 28));
int availableCores = Runtime.getRuntime().availableProcessors();
cores = availableCores < cores ? availableCores : cores > 0 ? cores : 1;
```
It is recommended to use a bit higher heap memory than 512M per core to avoid out of memory issues, however.

The second (optional) parameter is the explicit concurrency level.
If not provided, the application calculates the memory based on the heap memory assigned but not more than the maximum available cores.

## Unit tests

There are JUnit tests for the success case and the most of the error cases in the applications:
```
./gradlew clean test
```

## Possible improvements

As usual, every implementation can be improved.
There are several improvements to consider:
1. More efficient color finding algorithm
2. Off-heap storage of the working data for more efficient disposal
3. Back-pressure mechanism to cancel the pipeline in case of not enough memory for processing the image
4. Detecting color features from the image metadata (no need to download the whole image)
5. Detecting color palette of the near-duplicate images based on the fingerprinting
6. Horizontal scaling in terms of using multiple machines communicating over IPC, UDP, TCP, Multicast, etc.
   to split tasks and combine the results.  
