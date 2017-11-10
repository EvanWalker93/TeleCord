package main.java.TCBot;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.File;
import java.io.IOException;

public class Mp4ToGif {

    private FFmpeg ffmpeg = new FFmpeg("/usr/share/ffmpeg");
    private FFprobe fFprobe = new FFprobe("/usr/share/ffmpeg");
    private FFmpegBuilder builder = new FFmpegBuilder();


    public Mp4ToGif() throws IOException {
    }

    public boolean convert(String fileInput) {
        builder.setInput(fileInput)
                .overrideOutputFiles(true)
                .addOutput("file.gif")
                .setFormat("gif")
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, fFprobe);

        executor.createJob(builder).run();

        return true;
    }
}
