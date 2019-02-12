package com.colors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(BlockJUnit4ClassRunner.class)
public class Top3ColorsTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private final String output = new File(getClass().getClassLoader().getResource("output.csv").getFile())
            .getAbsolutePath();

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testTop3Colors_Success() {
        Top3Colors.main(new String[]{new File(getClass().getClassLoader().getResource("urls.txt").getFile())
                .getAbsolutePath(), output});
        assertTrue(outContent.toString().contains("Finished processing URLs"));
        assertTrue(errContent.toString().isEmpty());
    }

    @Test
    public void testTop3Colors_NoParameter() {
        Top3Colors.main(new String[]{});
        assertTrue(outContent.toString().contains("Top Three Colors Finder"));
        assertTrue(errContent.toString().isEmpty());
    }

    @Test
    public void testTop3Colors_IncorrectNumberOfArguments_TooMany() {
        Top3Colors.main(new String[]{"catch", "22", "is", "bad"});
        assertFalse(outContent.toString().contains("Finished processing URLs"));
        assertFalse(errContent.toString().isEmpty());
        assertTrue(errContent.toString().startsWith("Incorrect number of arguments, exiting!\n"));
    }

    @Test
    public void testTop3Colors_InvalidInputFile() {
        Top3Colors.main(new String[]{"invalid_file_name.txt", output, "1"});
        assertFalse(outContent.toString().contains("Finished processing URLs"));
        assertFalse(errContent.toString().isEmpty());
        assertTrue(errContent.toString().startsWith("Invalid input file name"));
    }

    @Test
    public void testTop3Colors_InvalidOutputFile() {
        Top3Colors.main(new String[]{"invalid_file_name.txt", "", "1"});
        assertFalse(outContent.toString().contains("Finished processing URLs"));
        assertFalse(errContent.toString().isEmpty());
        assertTrue(errContent.toString().startsWith("Cannot write to output file name"));
    }

    @Test
    public void testTop3Colors_ParallelismNotNumber() {
        Top3Colors.main(new String[]{"invalid_file_name.txt", output, "two"});
        assertFalse(outContent.toString().contains("Finished processing URLs"));
        assertFalse(errContent.toString().isEmpty());
        assertTrue(errContent.toString().startsWith("Concurrency level should be a number"));
    }

    @Test
    public void testTop3Colors_IncorrectLink_InvalidLink() {
        Top3Colors.main(new String[]{new File(getClass().getClassLoader().getResource("invalid_link.txt").getFile())
                .getAbsolutePath(), output});
        assertFalse(outContent.toString().contains("Finished processing URLs"));
        assertFalse(errContent.toString().isEmpty());
        assertTrue(errContent.toString().startsWith("Cannot process a URL"));
    }

    @Test
    public void testTop3Colors_IncorrectLink_NoImage() {
        Top3Colors.main(new String[]{new File(getClass().getClassLoader().getResource("no_image.txt").getFile())
                .getAbsolutePath(), output});
        assertFalse(outContent.toString().contains("Finished processing URLs"));
        assertFalse(errContent.toString().isEmpty());
        assertTrue(errContent.toString().startsWith("Cannot process a URL"));
    }
}