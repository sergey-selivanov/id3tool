package org.svs.id3tool.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.CharsetDetector;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

class Mp3Test {

    Logger log = LoggerFactory.getLogger(getClass());

    // https://stackoverflow.com/questions/11497902/how-to-check-the-charset-of-string-in-java

    private String convert(String value, Charset fromEncoding, Charset toEncoding) throws UnsupportedEncodingException {
        return new String(value.getBytes(fromEncoding), toEncoding);
    }

    private boolean probe(String value, Charset charset) throws UnsupportedEncodingException {
        Charset probe = StandardCharsets.UTF_8;
        //return value.equals(convert(convert(value, charset, probe), probe, charset));

        boolean result = value.equals(convert(convert(value, charset, probe), probe, charset));
        log.debug("{}: {}", charset.displayName(), result);
        return result;
    }

    public String convertGuess(String value, Charset charsetWanted, List<Charset> charsetsOther) throws UnsupportedEncodingException {
        if (probe(value, charsetWanted)) {
            return value;
        }
        for (Charset other: charsetsOther) {
            if (probe(value, other)) {
                return convert(value, other, charsetWanted);
            }
        }
        //System.err.println("WARNING: Could not convert string: " + value);
        log.error("Could not convert string: {}", value);
        return value;
    }

    @Test
    void testMp3() {

        List<String> fileNames = List.of(
                "I:\\music-new\\Александр Лаэртский\\1993 - Детства Чистые Глазенки\\01 - Детства Чистые Глазенки.mp3",
                "I:\\music-new\\Александр Лаэртский\\1993 - Детства Чистые Глазенки\\02 - Стопроцентная Смерть.mp3",
                "I:\\music-new\\#01_ZVEZDI_NAS_JDUT\\#1_01_Solnechnoe Leto.Mp3"
                );

        var charsets = Charset.availableCharsets();
        var listCharsets = List.copyOf(charsets.values());
//        listCharsets.forEach(cs ->{
//            log.debug("charset: {}", cs.displayName());
//        });

        fileNames.forEach(fileName -> {
            try {
                Mp3File mp3file = new Mp3File(fileName);

                log.debug("{}: v1 {}, v2 {}, custom {}",
                        mp3file.getFilename(),
                        mp3file.hasId3v1Tag(),
                        mp3file.hasId3v2Tag(),
                        mp3file.hasCustomTag()
                        );

                if(mp3file.hasId3v1Tag()) {
                    log.debug("v1: {}", mp3file.getId3v1Tag().getTitle());
                }

                if(mp3file.hasId3v2Tag()) {
                    ID3v2 tag = mp3file.getId3v2Tag();
                    tag.getFrameSets().forEach((k, v) ->{
                        //log.debug("frameset {}", k);
                        v.getFrames().forEach(fr -> {
//                            log.debug("frame {} datalen {}", fr.getId(), fr.getDataLength());

                            if("TIT2".equals(fr.getId())) {
                                //data = fr.getData();
                                //new String(fr.getData());

                                var match = new CharsetDetector().setText(fr.getData()).detect();
                                log.debug("detected {}", match.getName());
                                log.debug("== decoded {}", new String(fr.getData(), Charset.forName(match.getName())));
                            }
                        });
                    });
                    log.debug("v2: {}", tag.getTitle());
                    //log.debug("converted: {}", convertGuess(mp3file.getId3v2Tag().getTitle(), StandardCharsets.UTF_8, listCharsets) );
                    log.debug("converted: {}", convert(mp3file.getId3v2Tag().getTitle(), Charset.forName("windows-1251"), StandardCharsets.UTF_8) );
                }

            } catch (UnsupportedTagException | InvalidDataException | IOException e) {
                log.error("failed", e);
            }
        });

        //fail("Not yet implemented");
    }

}
