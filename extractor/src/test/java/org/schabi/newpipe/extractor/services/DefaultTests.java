package org.schabi.newpipe.extractor.services;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Calendar;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;
import static org.schabi.newpipe.extractor.ExtractorAsserts.*;
import static org.schabi.newpipe.extractor.StreamingService.*;

public final class DefaultTests {
    public static void defaultTestListOfItems(StreamingService expectedService, List<? extends InfoItem> itemsList, List<Throwable> errors) throws ParsingException {
        assertFalse("List of items is empty", itemsList.isEmpty());
        assertFalse("List of items contains a null element", itemsList.contains(null));
        assertEmptyErrors("Errors during extraction", errors);

        for (InfoItem item : itemsList) {
            assertIsSecureUrl(item.getUrl());
            if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isEmpty()) {
                assertIsSecureUrl(item.getThumbnailUrl());
            }
            assertNotNull("InfoItem type not set: " + item, item.getInfoType());
            assertEquals("Unexpected item service id", expectedService.getServiceId(), item.getServiceId());
            assertNotEmpty("Item name not set: " + item, item.getName());

            if (item instanceof StreamInfoItem) {
                StreamInfoItem streamInfoItem = (StreamInfoItem) item;
                assertNotEmpty("Uploader name not set: " + item, streamInfoItem.getUploaderName());
                assertNotEmpty("Uploader url not set: " + item, streamInfoItem.getUploaderUrl());
                assertIsSecureUrl(streamInfoItem.getUploaderUrl());

                assertExpectedLinkType(expectedService, streamInfoItem.getUrl(), LinkType.STREAM);
                assertExpectedLinkType(expectedService, streamInfoItem.getUploaderUrl(), LinkType.CHANNEL);

                final String textualUploadDate = streamInfoItem.getTextualUploadDate();
                if (textualUploadDate != null && !textualUploadDate.isEmpty()) {
                    final DateWrapper uploadDate = streamInfoItem.getUploadDate();
                    assertNotNull("No parsed upload date", uploadDate);
                    assertTrue("Upload date not in the past", uploadDate.date().before(Calendar.getInstance()));
                }

            } else if (item instanceof ChannelInfoItem) {
                final ChannelInfoItem channelInfoItem = (ChannelInfoItem) item;
                assertExpectedLinkType(expectedService, channelInfoItem.getUrl(), LinkType.CHANNEL);

            } else if (item instanceof PlaylistInfoItem) {
                final PlaylistInfoItem playlistInfoItem = (PlaylistInfoItem) item;
                assertExpectedLinkType(expectedService, playlistInfoItem.getUrl(), LinkType.PLAYLIST);
            }
        }
    }

    private static void assertExpectedLinkType(StreamingService expectedService, String url, LinkType expectedLinkType) throws ParsingException {
        final LinkType linkTypeByUrl = expectedService.getLinkTypeByUrl(url);

        assertNotEquals("Url is not recognized by its own service: \"" + url + "\"",
                LinkType.NONE, linkTypeByUrl);
        assertEquals("Service returned wrong link type for: \"" + url + "\"",
                expectedLinkType, linkTypeByUrl);
    }

    public static <T extends InfoItem> void assertNoMoreItems(ListExtractor<T> extractor) throws Exception {
        assertFalse("More items available when it shouldn't", extractor.hasNextPage());
        final String nextPageUrl = extractor.getNextPageUrl();
        assertTrue("Next page is not empty or null", nextPageUrl == null || nextPageUrl.isEmpty());
    }

    public static <T extends InfoItem> ListExtractor.InfoItemsPage<T> defaultTestRelatedItems(ListExtractor<T> extractor) throws Exception {
        final ListExtractor.InfoItemsPage<T> page = extractor.getInitialPage();
        final List<T> itemsList = page.getItems();
        List<Throwable> errors = page.getErrors();

        defaultTestListOfItems(extractor.getService(), itemsList, errors);
        return page;
    }

    public static <T extends InfoItem> ListExtractor.InfoItemsPage<T> defaultTestMoreItems(ListExtractor<T> extractor) throws Exception {
        assertTrue("Doesn't have more items", extractor.hasNextPage());
        ListExtractor.InfoItemsPage<T> nextPage = extractor.getPage(extractor.getNextPageUrl());
        final List<T> items = nextPage.getItems();
        assertFalse("Next page is empty", items.isEmpty());
        assertEmptyErrors("Next page have errors", nextPage.getErrors());

        defaultTestListOfItems(extractor.getService(), nextPage.getItems(), nextPage.getErrors());
        return nextPage;
    }

    public static void defaultTestGetPageInNewExtractor(ListExtractor<? extends InfoItem> extractor, ListExtractor<? extends InfoItem> newExtractor) throws Exception {
        final String nextPageUrl = extractor.getNextPageUrl();

        final ListExtractor.InfoItemsPage<? extends InfoItem> page = newExtractor.getPage(nextPageUrl);
        defaultTestListOfItems(extractor.getService(), page.getItems(), page.getErrors());
    }
}
