package com.nyala.media.input.spi;

import com.nyala.media.input.live.LiveInputStream;

public interface LiveMediaIngester {

    void ingestLiveMedia(LiveInputStream liveInputStream);
}
