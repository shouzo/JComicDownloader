/*
----------------------------------------------------------------------------------------------------
This class is currently maintained by hkgsherlock. Please report any problems or giving improvements on GitHub https://github.com/abc9070410/JComicDownloader/issues .
----------------------------------------------------------------------------------------------------
ChangeLog:
5.19: Fixed getting first image for twice, that of last not fetched problem.
5.19: Changed de-obfuscation algorithm due to change of the 8comic site.
5.17: 修復8comic改變位址的問題。
5.16: 修復8comic解析失敗的問題。
5.06: 修復8comic因網站改版而解析錯誤的問題。
5.02: 修復8comic因網站改版而解析錯誤的問題。
2.09: 新增對6comic.com的支援。
1.17: 修復集數名稱後面數字會消失的bug。
1.08: 增加對於8comic的支援，包含免費漫畫和圖庫
----------------------------------------------------------------------------------------------------
 */
package jcomicdownloader.module;

import jcomicdownloader.tools.*;
import jcomicdownloader.enums.*;
import jcomicdownloader.*;

import java.util.*;
import jcomicdownloader.encode.Zhcode;
import org.jsoup.nodes.*;
import org.jsoup.parser.*;
import org.jsoup.select.*;


public class ParseEC extends ParseOnlineComicSite {
    protected String jsName;
    protected String indexWrongEncodingFileName;
    protected String indexFileName;
    private String volumeNoString; // 每一集都有數字編號

    public ParseEC() {
        siteID = Site.EIGHT_COMIC;
        siteName = "8comic";
        indexWrongEncodingFileName = Common.getStoredFileName( SetUp.getTempDirectory(), "index_8comic_wrong_encode_parse_", "html" );
        indexFileName = Common.getStoredFileName( SetUp.getTempDirectory(), "index_8comic_parse_", "html" );

        jsName = "index_8comic.js";
        volumeNoString = "";
    }

    public ParseEC( String webSite, String titleName ) {
        this();
        this.webSite = webSite;
        this.title = titleName;
    }

    @Override
    public void setParameters() { // let all the non-set attributes get values
        Common.debugPrintln( "開始解析各參數 :" );

        Common.debugPrintln("開始解析title和wholeTitle :");

        Common.downloadFile(webSite, SetUp.getTempDirectory(), indexWrongEncodingFileName, false, "", "");
        Common.simpleDownloadFile(webSite, SetUp.getTempDirectory(), indexWrongEncodingFileName, webSite);
        Common.newEncodeFile(SetUp.getTempDirectory(), indexWrongEncodingFileName, indexFileName, Zhcode.BIG5);
        Common.deleteFile(indexWrongEncodingFileName);
        String allPageString = Common.getFileString( SetUp.getTempDirectory(), indexFileName);
        
        // ex. http://www.8comic.com/love/drawing-8170.html?ch=3
        volumeNoString = webSite.split( "/|=" )[webSite.split( "/|=" ).length - 1];

        if ( getWholeTitle() == null || getWholeTitle().equals( "" ) ) {
            setWholeTitle( getTitle() + volumeNoString );
        }

        Common.debugPrintln( "作品名稱(title) : " + getTitle() );
        Common.debugPrintln( "章節名稱(wholeTitle) : " + getWholeTitle() );
    }

    @Override
    public void parseComicURL() { // parse URL and save all URLs in comicURL
        
        //取得ch
        int beginIndex = 0;
        int endIndex = 0;
        
        String ch = "1";
        if ( webSite.indexOf( "=" ) > 0 )
        {
            beginIndex = webSite.indexOf( "=" ) + 1;
            endIndex = webSite.length();
            ch = webSite.substring( beginIndex, endIndex );
        }
        Common.debugPrintln( "ch: " + ch );

        String allPageString = Common.getFileString( SetUp.getTempDirectory(), indexFileName);
        
        // 取得chs
        beginIndex = allPageString.indexOf( "var chs" );
        beginIndex = allPageString.indexOf( "=", beginIndex ) + 1;
        endIndex = allPageString.indexOf( ";", beginIndex );
        String chs = allPageString.substring( beginIndex, endIndex );
        Common.debugPrintln( "chs: " + chs );
        
        // 取得itemid
        beginIndex = allPageString.indexOf( "var ti" );
        beginIndex = allPageString.indexOf( "=", beginIndex ) + 1;
        endIndex = allPageString.indexOf( ";", beginIndex );
        String itemid = allPageString.substring( beginIndex, endIndex );
        Common.debugPrintln( "itemid(ti): " + itemid );
        
        // 取得圖片編碼
        beginIndex = allPageString.indexOf( "var cs", beginIndex );
        beginIndex = allPageString.indexOf( "\'", beginIndex ) + 1;
        endIndex = allPageString.indexOf( "\'", beginIndex );
        String allcodes = allPageString.substring( beginIndex, endIndex );

        // use re-gened JS for de-obfuscation
        NView_Java nv = new NView_Java(Integer.parseInt(chs), Integer.parseInt(itemid), allcodes, ch);
        this.comicURL = new String[nv.getPagesCount()];
        nv.setPage(1);
        // must be started from 1 since this index follows the real page number
        for (int d = 1; d <= nv.getPagesCount(); nv.setPage(++d)) {
            this.comicURL[d - 1] = nv.parse();
        }
    }

    @Override
    public String getAllPageString( String urlString ) {
        String indexWrongEncodingFileName = Common.getStoredFileName( SetUp.getTempDirectory(), "index_8comic_", "html" );
        String indexFileName = Common.getStoredFileName( SetUp.getTempDirectory(), "index_8comic_wrong_encode_", "html" );
        Common.downloadFile( urlString, SetUp.getTempDirectory(), indexWrongEncodingFileName, false, "" );
        Common.newEncodeFile(SetUp.getTempDirectory(), indexWrongEncodingFileName, indexFileName, Zhcode.BIG5);
        Common.deleteFile(indexWrongEncodingFileName);

        return Common.getFileString( SetUp.getTempDirectory(), indexFileName ).replace( "&#22338;", "阪" );
    }

    @Override
    public boolean isSingleVolumePage( String urlString ) {
        return urlString.matches( "(?s).*/show/(?s).*" ); // ex. http://www.8comic.com/love/drawing-2245.html?ch=51
    }

    @Override
    public String getTitleOnSingleVolumePage( String urlString ) {
        // http://www.8comic.com/love/drawing-8170.html?ch=2轉為http://www.8comic.com/html/8170.html

        String[] splitURLs = urlString.split( "://|/|-|\\?" );

        String baseURL = "http://www.8comic.com/html/";
        String mainPageUrlString = baseURL + splitURLs[4];

        return getTitleOnMainPage( mainPageUrlString, getAllPageString( mainPageUrlString ) );
    }

    @Override
    public String getTitleOnMainPage( String urlString, String allPageString ) {
        Document nodes = Parser.parse(allPageString, urlString);
        String ret = nodes.select("body > table:nth-of-type(2) table table:first-of-type tr:first-of-type font").text();
        return ret;
    }

    @Override
    public List<List<String>> getVolumeTitleAndUrlOnMainPage( String urlString, String allPageString ) {
        Document nodes = Parser.parse(allPageString, urlString);

        // combine volumeList and urlList into combinationList, return it.
        List<List<String>> combinationList = new ArrayList<List<String>>();
        List<String> urlList = new ArrayList<String>();
        List<String> volumeList = new ArrayList<String>();

        Elements linksToEpisodes = nodes.select("#rp_tb_comic_0 table a.Vol, a.Ch");
        totalVolume = linksToEpisodes.size();
        Common.debugPrintln( "共有" + totalVolume + "集" );

        for (Element ele : linksToEpisodes) {
            ele.attributes();
            String strJsEnterPageArgs = ele.attr("onclick");
            strJsEnterPageArgs = strJsEnterPageArgs.substring(strJsEnterPageArgs.indexOf("cview(") + 6, strJsEnterPageArgs.length() - ");return false;".length());
            String[] jsEnterPageArgs = strJsEnterPageArgs.split(",");

            // ex. cview('2245-49.html' 取 2245-49.html
            String[] idAndVolume = jsEnterPageArgs[0].replace("'", "").split( "-|\\." );
            // ex.cview('104-97.html',8) -> 取8
            String catid = jsEnterPageArgs[1].trim();

            // get URLs of every single episodes
            String strId = idAndVolume[0];
            String strVolume = idAndVolume[1];
            urlList.add(getSinglePageURL(strId, strVolume, catid));

            String volumeTitle = ele.text().trim();

            // fix until being reported, no example to test
//            if ( volumeTitle == null || volumeTitle.equals("")  )
//            {
//                beginIndex = allPageString.indexOf( ">", beginIndex ) + 1;
//                endIndex = allPageString.indexOf( "<", beginIndex );
//                volumeTitle = allPageString.substring( beginIndex, endIndex ).trim();
//            }

            volumeTitle = getVolumeWithFormatNumber( Common.getStringRemovedIllegalChar(
                    Common.getTraditionalChinese( volumeTitle.trim() ) ) );
            volumeList.add( getVolumeWithFormatNumber(volumeTitle) );
        }

        combinationList.add( volumeList );
        combinationList.add( urlList );

        return combinationList;
    }

    // 取得單集頁面的網址
    public String getSinglePageURL( String idString, String volumeNoString, String catidString ) {

        String ret = "";

        switch (Integer.parseInt( catidString )) {
            case 4:
            case 6:
            case 12:
            case 22:

            case 1:
            case 17:
            case 19:
            case 21:

            case 2:
            case 5:
            case 7:
            case 9:
                ret += "http://new.comicvip.com/show/cool-";
                break;
            case 10:
            case 11:
            case 13:
            case 14:

            case 3:
            case 8:
            case 15:
            case 16:
            case 18:
            case 20:
                ret += "http://new.comicvip.com/show/best-manga-";
                break;
            default:
                throw new IllegalArgumentException("The catid is not whithin the valid range.");
        }

        ret += idString + ".html?ch=" + volumeNoString;
        
        return ret;
    }

    @Override
    public void printLogo() {
        System.out.println( " _____________________________" );
        System.out.println( "|                          " );
        System.out.println( "| Run the 8comic module: " );
        System.out.println( "|______________________________\n" );
    }

    @Override
    public String getMainUrlFromSingleVolumeUrl( String volumeURL ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}

class ParseECphoto extends ParseEC {

    public ParseECphoto() {
        siteID = Site.EIGHT_COMIC_PHOTO;
        indexWrongEncodingFileName = Common.getStoredFileName( SetUp.getTempDirectory(), "index_8comic_photo_parse_", "html" );
        indexFileName = Common.getStoredFileName( SetUp.getTempDirectory(), "index_8comic_photo_encode_parse_", "html" );

        jsName = "index_8comic_photo.js";
    }

    @Override
    public void setParameters() { // let all the non-set attributes get values
        Common.debugPrintln( "開始解析各參數 :" );

        Common.debugPrintln( "開始解析title和wholeTitle :" );

        String allPageString = getAllPageString( webSite );

        if ( this.getWholeTitle() == null || this.getWholeTitle().equals( "" ) ) {
            int beginIndex = allPageString.indexOf( "<title>" ) + 7;
            int endIndex = allPageString.indexOf( "</title>", beginIndex );
            String titleString = allPageString.substring( beginIndex, endIndex );

            this.setWholeTitle( getVolumeWithFormatNumber(
                    Common.getStringRemovedIllegalChar( titleString ) ) );
        }

        Common.debugPrintln( "作品名稱(title) : " + getTitle() );
        Common.debugPrintln( "章節名稱(wholeTitle) : " + getWholeTitle() );
    }

    @Override
    public void parseComicURL() { // parse URL and save all URLs in comicURL
        // 先取得前面的下載伺服器網址
        String allPageString = getAllPageString( webSite );
//        Document doc = Parser.parse(getAllPageString(webSite), webSite);

        Common.debugPrint( "開始解析這一集有幾頁 :" );
        totalPage = allPageString.split( "\\.jpe'" ).length - 1;
        comicURL = new String[totalPage];
        Common.debugPrint( "共" + totalPage + "頁" );

        String[] tokens = allPageString.split( "'|\\." );

        int page = 0;
        for ( int i = 0 ; i < tokens.length ; i++ ) {
            if ( tokens[i].equals( "jpe" ) ) {
                comicURL[page++] = "http://www.8comic.com" + tokens[i - 1] + ".jpg";
                //Common.debugPrintln( (page-1) + " " + comicURL[page-1] );
            }
        }

        //System.exit(0);
    }

    @Override
    public boolean isSingleVolumePage( String urlString ) {
        return !urlString.matches( "(?s).*\\d+-\\d+.html(?s).*" );
    }

    @Override
    public String getTitleOnSingleVolumePage( String urlString ) {
        return "8comic圖集";
    }

    @Override
    public String getTitleOnMainPage( String urlString, String allPageString ) {
        return "8comic圖集";
    }

    @Override
    public List<List<String>> getVolumeTitleAndUrlOnMainPage( String urlString, String allPageString ) {
        // combine volumeList and urlList into combinationList, return it.

        List<List<String>> combinationList = new ArrayList<List<String>>();
        List<String> urlList = new ArrayList<String>();
        List<String> volumeList = new ArrayList<String>();

        int beginIndex = allPageString.indexOf( "id=\"newphoto_dl" );
        int endIndex = allPageString.indexOf( "id=\"newphoto_pager", beginIndex );
        String tempString = allPageString.substring( beginIndex, endIndex );
        String[] tempStrings = tempString.split( "\\d*>\\d*|\\d*<\\d*|\"" );

        totalVolume = tempString.split( "href=" ).length - 1;
        Common.debugPrintln( "共有" + totalVolume + "個圖集" );

        int nowVolume = 0;
        for ( int i = 0 ; i < tempStrings.length ; i++ ) {
            if ( tempStrings[i].matches( "(?s).*href=(?s).*" ) ) {
                urlList.add( "http://www.8comic.com" + tempStrings[i + 1] );
            } else if ( tempStrings[i].matches( "(?s).*br.*" ) ) {
                volumeList.add( tempStrings[i + 1].trim() );
            }
        }

        combinationList.add( volumeList );
        combinationList.add( urlList );

        return combinationList;
    }
}

class ParseSixComic extends ParseEC {

    @Override
    public String getTitleOnSingleVolumePage( String urlString ) {
        // http://www.8comic.com/love/drawing-7853.html?ch=1 轉為
        // http://www.6comic.com/comic/manga-7853.html

        String[] splitURLs = urlString.split( "-|\\?" );

        String baseURL = "http://www.6comic.com/comic/manga-";
        String mainPageUrlString = baseURL + splitURLs[1];

        return getTitleOnMainPage( mainPageUrlString, getAllPageString( mainPageUrlString ) );
    }
    
    @Override
    public List<List<String>> getVolumeTitleAndUrlOnMainPage( String urlString, String allPageString ) {
        // combine volumeList and urlList into combinationList, return it.

        List<List<String>> combinationList = new ArrayList<List<String>>();
        List<String> urlList = new ArrayList<String>();
        List<String> volumeList = new ArrayList<String>();

        int beginIndex = allPageString.indexOf( "comicview.js\"" );
        int endIndex = allPageString.indexOf( "id=\"tb_anime\"", beginIndex );
        String tempString = allPageString.substring( beginIndex, endIndex );

        totalVolume = tempString.split( "onmouseover=" ).length - 1;
        Common.debugPrintln( "共有" + totalVolume + "集" );


        String idString = ""; // ex. 7853
        String volumeNoString = ""; // ex. 3
        String volumeTitle = "";

        beginIndex = endIndex = 0;
        for ( int i = 0 ; i < totalVolume ; i++ ) {
            // 取得單集位址
            beginIndex = tempString.indexOf( "onmouseover=", beginIndex );
            beginIndex = tempString.indexOf( "'", beginIndex ) + 1;
            endIndex = tempString.indexOf( "'", beginIndex );
            idString = tempString.substring( beginIndex, endIndex );

            beginIndex = tempString.indexOf( "'", endIndex + 1 ) + 1;
            endIndex = tempString.indexOf( "'", beginIndex );
            volumeNoString = tempString.substring( beginIndex, endIndex );

            urlList.add( getSinglePageURL( idString, volumeNoString ) );


            // 取得單集名稱
            beginIndex = tempString.indexOf( ">", endIndex ) + 1;
            endIndex = tempString.indexOf( "<", beginIndex );

            volumeTitle = getVolumeWithFormatNumber( Common.getStringRemovedIllegalChar(
                    Common.getTraditionalChinese( tempString.substring( beginIndex, endIndex ).trim() ) ) );
            volumeList.add( getVolumeWithFormatNumber( volumeTitle ) );

        }

        combinationList.add( volumeList );
        combinationList.add( urlList );

        return combinationList;
    }

    // 取得單集頁面的網址
    public String getSinglePageURL( String idString, String volumeNoString ) {

        String baseMainURL = "http://www.8comic.com/love/drawing-";
        String volumeString = "?ch=" + volumeNoString;

        return baseMainURL + idString + ".html" + volumeString;
    }

    @Override
    public void printLogo() {
        System.out.println( " ____________________________________" );
        System.out.println( "|                                 " );
        System.out.println( "| Run the 6comic module: " );
        System.out.println( "|_____________________________________\n" );
    }
}

/**
 * The Java Class of translated JavaScript file "http://new.comicvip.com//js/nview.js"
 * @author hkgsherlock
 */
class NView_Java {
    /**
     * Storing the result URL to the image of that page of manga.
     */
    private String urlResult;

    /**
     * Number of chapters available in this manga.
     */
    private final int chs;

    /**
     * The numeric identifier (ID) of the manga.
     */
    private final int ti;

    /**
     * Compiled String stored in JavaScript variable exactly named as "cs" in Page View page.
     */
    private final String cs;
    /**
     * String of chapter, also found on URL param "ch". Format: "1", "1-3", "153-12"
     */
    private String ch;

    public NView_Java(int chapters, int mangaId, String compiledString, String chapter) {
        this.chs = chapters;
        this.ti = mangaId;
        this.cs = compiledString;
        this.ch = chapter;

        if (ch.indexOf('-') > 0) {
            p = Integer.parseInt(ch.split("-")[1]);
            ch = ch.split("-")[0];
        }
        
        this.sp();
    }

    public NView_Java(int chapters, int mangaId, String compiledString, int chapter, int page) {
        this.chs = chapters;
        this.ti = mangaId;
        this.cs = compiledString;
        this.ch = chapter + "";
        this.p = page;
        
        this.sp();
    }

    /**
     * Parse all sort of stuffs as a URL to the manga page image.
     * @return The image of that page of a manga.
     */
    public String parse() {
        si(c);
        return this.urlResult;
    }

    /**
     * Get pages available for this volume of manga.
     * <p><em>Note:</em> set the </p>
     * @return Count of pages of the volume.
     */
    public int getPagesCount() {
        String strCnt = ss(c, 7, 3);
        return Integer.parseInt(strCnt);
    }

    /**
     * Set the chapter for the following work.
     * @param chapter The number of chapter to work.
     */
    public void setChapter(int chapter) {
        this.ch = chapter + "";
    }

    /**
     * Set the page to work.
     * @param page The number of page to work.
     */
    public void setPage(int page) {
        this.p = page;
    }

    private String c = "";
    private final int f = 50;
    private int p = 1;

    /**
     * The first function called by the 8comic view page, which provides ability to decode the "cs"
     * (compiled string) in the page js to a image link hyper-referenced by img#TheImg DOM.
     */
    private void sp() {

        int cc = cs.length();
        for (int i = 0; i < cc / f; i++) {
            if (ss(cs, i * f, 4).equals(ch)) {
                c = ss(cs, i * f, f, f);
                break;
            }
        }
        if (c.equals("")) {
            c = ss(cs, cc - f, f);
            ch = chs + "";
        }
    }

    private String ss(String a, int b, int c) {
        return ss(a, b, c, null);
    }

    private String ss(String a, int b, int c, Object d) {
        String e = a.substring(b, b + c);
        return (d == null) ? e.replaceAll("[a-z]*", "") : e;
    }

    /**
     * Padding zero for a integer, letting the string of number becomes one which length of 3.
     */
    private String nn(int n) {
        return String.format("%03d", n);
    }

    private int mm(int p) {
        //noinspection RedundantCast
        return ((int)((p - 1) / 10) % 10) + (((p - 1) % 10) * 3);
    }

    private void si(String c) {
        this.urlResult = "http://img" + ss(c, 4, 2) + ".8comic.com/" + ss(c, 6, 1) + "/" + ti + "/" + ss(c, 0, 4) + "/" + nn(p) + "_" + ss(c, mm(p) + 10, 3, f) + ".jpg";
    }
}
