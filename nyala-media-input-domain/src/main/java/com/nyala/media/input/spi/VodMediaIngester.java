package com.nyala.media.input.spi;

import com.nyala.media.input.VodMedia;

import java.util.List;

public interface VodMediaIngester {

    void ingestVodMedia(VodMedia vodMedia);
    void bulkIngest(List<VodMedia> vodMediaList);

}
