package com.smartprep.dto;

public class VideoDTO {
    private String subject;
    private String chapter;
    private String videoTitle;
    private String channel;
    private String youtubeUrl;

    public VideoDTO() {}

    public VideoDTO(String subject, String chapter, String videoTitle, String channel, String youtubeUrl) {
        this.subject = subject;
        this.chapter = chapter;
        this.videoTitle = videoTitle;
        this.channel = channel;
        this.youtubeUrl = youtubeUrl;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
}