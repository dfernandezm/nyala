package com.nyala.media.input.spi;

import com.nyala.media.input.Media;
import com.nyala.media.input.MediaId;

public interface MediaRepository {

    void save(Media media);
    void delete(MediaId mediaId);
}
