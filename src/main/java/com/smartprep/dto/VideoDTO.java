package com.smartprep.dto;

public class VideoDTO {
    private String subject;
    private String chapter;
    private String title;
    private String channel;
    private String videoUrl;
    private String thumbnail;

    public VideoDTO() {}

    public VideoDTO(String subject, String chapter, String title, String channel, String videoUrl) {
        this.subject = subject;
        this.chapter = chapter;
        this.title = title;
        this.channel = channel;
        this.videoUrl = videoUrl;
    }

    public VideoDTO(String subject, String chapter, String title, String channel, String videoUrl, String thumbnail) {
        this.subject = subject;
        this.chapter = chapter;
        this.title = title;
        this.channel = channel;
        this.videoUrl = videoUrl;
        this.thumbnail = thumbnail;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
}