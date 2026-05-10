package com.ych.contentfactory.ppt;

import com.ych.contentfactory.model.PresentationPlan;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFBackground;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 使用 Apache POI 生成与 Python 示例相近的科技风深色幻灯片。
 * 新建演示文稿须使用 {@link XMLSlideShow}（{@code XSLFSlideShow} 无无参构造器）。
 */
public final class PptxBuilder {

    private static final Color BG = new Color(0x0A1628);
    private static final Color ACCENT = new Color(0x00D4FF);
    private static final Color TEXT = Color.WHITE;
    private static final Color SUBTEXT = new Color(0xB0C4DE);

    private PptxBuilder() {
    }

    public static void write(PresentationPlan plan, Path pptxPath) throws Exception {
        Files.createDirectories(pptxPath.getParent());
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(960, 540));

            addTitleSlide(ppt, plan.title, plan.subtitle);

            for (PresentationPlan.BodySlide s : plan.bodySlides) {
                addContentSlide(ppt, s.heading, s.bullets);
            }

            addClosingSlide(ppt, plan.closingNarration);

            try (OutputStream out = Files.newOutputStream(pptxPath)) {
                ppt.write(out);
            }
        }
    }

    private static void paintBackground(XSLFSlide slide) {
        XSLFBackground bg = slide.getBackground();
        if (bg != null) {
            bg.setFillColor(BG);
        }
    }

    private static void addTitleSlide(XMLSlideShow ppt, String title, String subtitle) {
        XSLFSlide slide = ppt.createSlide();
        paintBackground(slide);

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle2D.Double(60, 160, 840, 120));
        titleBox.clearText();
        XSLFTextParagraph tp = titleBox.addNewTextParagraph();
        XSLFTextRun tr = tp.addNewTextRun();
        tr.setText(title);
        tr.setFontSize(40.0);
        tr.setBold(true);
        tr.setFontColor(ACCENT);

        if (subtitle != null && !subtitle.isBlank()) {
            XSLFTextBox sub = slide.createTextBox();
            sub.setAnchor(new Rectangle2D.Double(60, 290, 840, 80));
            sub.clearText();
            XSLFTextParagraph sp = sub.addNewTextParagraph();
            XSLFTextRun sr = sp.addNewTextRun();
            sr.setText(subtitle);
            sr.setFontSize(22.0);
            sr.setFontColor(SUBTEXT);
        }
    }

    private static void addContentSlide(XMLSlideShow ppt, String heading, List<String> bullets) {
        XSLFSlide slide = ppt.createSlide();
        paintBackground(slide);

        XSLFTextBox head = slide.createTextBox();
        head.setAnchor(new Rectangle2D.Double(40, 36, 880, 64));
        head.clearText();
        XSLFTextParagraph hp = head.addNewTextParagraph();
        XSLFTextRun hr = hp.addNewTextRun();
        hr.setText(heading == null ? "" : heading);
        hr.setFontSize(28.0);
        hr.setBold(true);
        hr.setFontColor(ACCENT);

        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(56, 110, 848, 380));
        box.clearText();
        List<String> pts = bullets == null ? List.of() : bullets;
        for (String pt : pts) {
            XSLFTextParagraph p = box.addNewTextParagraph();
            p.setSpaceAfter(10.0);
            XSLFTextRun r = p.addNewTextRun();
            r.setText("• " + pt);
            r.setFontSize(20.0);
            r.setFontColor(TEXT);
        }
    }

    private static void addClosingSlide(XMLSlideShow ppt, String summary) {
        XSLFSlide slide = ppt.createSlide();
        paintBackground(slide);

        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(80, 160, 800, 160));
        box.clearText();
        XSLFTextParagraph p = box.addNewTextParagraph();
        XSLFTextRun r = p.addNewTextRun();
        r.setText(summary == null ? "谢谢" : summary);
        r.setFontSize(24.0);
        r.setFontColor(TEXT);

        XSLFTextBox thanks = slide.createTextBox();
        thanks.setAnchor(new Rectangle2D.Double(80, 340, 800, 60));
        thanks.clearText();
        XSLFTextParagraph tp = thanks.addNewTextParagraph();
        XSLFTextRun tr = tp.addNewTextRun();
        tr.setText("感谢观看");
        tr.setFontSize(22.0);
        tr.setFontColor(ACCENT);
    }
}
