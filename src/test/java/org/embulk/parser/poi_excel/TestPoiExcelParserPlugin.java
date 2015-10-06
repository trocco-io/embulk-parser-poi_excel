package org.embulk.parser.poi_excel;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaError;
import org.embulk.config.ConfigSource;
import org.embulk.parser.EmbulkPluginTester;
import org.embulk.parser.EmbulkTestOutputPlugin.OutputRecord;
import org.embulk.parser.EmbulkTestParserConfig;
import org.embulk.spi.time.Timestamp;
import org.junit.Test;

public class TestPoiExcelParserPlugin {

	@Test
	public void test1() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "test1");
			parser.set("skip_header_lines", 1);
			parser.set("default_timezone", "Asia/Tokyo");
			parser.addColumn("boolean", "boolean");
			parser.addColumn("long", "long");
			parser.addColumn("double", "double");
			parser.addColumn("string", "string");
			parser.addColumn("timestamp", "timestamp").set("format", "%Y/%m/%d");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(7));
			check1(result, 0, true, 123L, 123.4d, "abc", "2015/10/4");
			check1(result, 1, false, 456L, 456.7d, "def", "2015/10/5");
			check1(result, 2, false, 123L, 123d, "456", "2015/10/6");
			check1(result, 3, true, 123L, 123.4d, "abc", "2015/10/7");
			check1(result, 4, true, 123L, 123.4d, "abc", "2015/10/4");
			check1(result, 5, true, 1L, 1d, "true", null);
			check1(result, 6, null, null, null, null, null);
		}
	}

	private SimpleDateFormat sdf;
	{
		sdf = new SimpleDateFormat("yyyy/MM/dd");
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
	}

	private void check1(List<OutputRecord> result, int index, Boolean b, Long l, Double d, String s, String t)
			throws ParseException {
		Timestamp timestamp = (t != null) ? Timestamp.ofEpochMilli(sdf.parse(t).getTime()) : null;

		OutputRecord r = result.get(index);
		// System.out.println(r);
		assertThat(r.getAsBoolean("boolean"), is(b));
		assertThat(r.getAsLong("long"), is(l));
		assertThat(r.getAsDouble("double"), is(d));
		assertThat(r.getAsString("string"), is(s));
		assertThat(r.getAsTimestamp("timestamp"), is(timestamp));
	}

	@Test
	public void testColumnNumber_string() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "test1");
			parser.set("skip_header_lines", 1);
			parser.set("cell_error_null", false);
			parser.addColumn("text", "string").set("column_number", "D");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(7));
			assertThat(result.get(0).getAsString("text"), is("abc"));
			assertThat(result.get(1).getAsString("text"), is("def"));
			assertThat(result.get(2).getAsString("text"), is("456"));
			assertThat(result.get(3).getAsString("text"), is("abc"));
			assertThat(result.get(4).getAsString("text"), is("abc"));
			assertThat(result.get(5).getAsString("text"), is("true"));
			assertThat(result.get(6).getAsString("text"), is("#DIV/0!"));
		}
	}

	@Test
	public void testColumnNumber_int() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "test1");
			parser.set("skip_header_lines", 1);
			parser.set("cell_error_null", false);
			parser.addColumn("long", "long").set("column_number", 2);
			parser.addColumn("double", "double");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(7));
			check3(result, 0, 123L, 123.4d);
			check3(result, 1, 456L, 456.7d);
			check3(result, 2, 123L, 123d);
			check3(result, 3, 123L, 123.4d);
			check3(result, 4, 123L, 123.4d);
			check3(result, 5, 1L, 1d);
			check3(result, 6, (long) FormulaError.DIV0.getCode(), (double) FormulaError.DIV0.getCode());
		}
	}

	private void check3(List<OutputRecord> result, int index, Long l, Double d) throws ParseException {
		OutputRecord r = result.get(index);
		// System.out.println(r);
		assertThat(r.getAsLong("long"), is(l));
		assertThat(r.getAsDouble("double"), is(d));
	}

	@Test
	public void testRowNumber() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "test1");
			parser.set("skip_header_lines", 1);
			parser.set("cell_error_null", false);
			parser.addColumn("sheet", "string").set("value_type", "sheet_name");
			parser.addColumn("row", "long").set("value_type", "row_number");
			parser.addColumn("flag", "boolean");
			parser.addColumn("col-n", "long").set("value_type", "column_number");
			parser.addColumn("col-s", "string").set("value_type", "column_number");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(7));
			check4(result, 0, "test1", true);
			check4(result, 1, "test1", false);
			check4(result, 2, "test1", false);
			check4(result, 3, "test1", true);
			check4(result, 4, "test1", true);
			check4(result, 5, "test1", true);
			check4(result, 6, "test1", null);
		}
	}

	private void check4(List<OutputRecord> result, int index, String sheetName, Boolean b) {
		OutputRecord r = result.get(index);
		// System.out.println(r);
		assertThat(r.getAsString("sheet"), is(sheetName));
		assertThat(r.getAsLong("row"), is((long) (index + 2)));
		assertThat(r.getAsBoolean("flag"), is(b));
		assertThat(r.getAsLong("col-n"), is(1L));
		assertThat(r.getAsString("col-s"), is("A"));
	}

	@Test
	public void testForumlaReplace() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "formula_replace");

			ConfigSource replace0 = tester.newConfigSource();
			replace0.set("regex", "test1");
			replace0.set("to", "merged_cell");
			ConfigSource replace1 = tester.newConfigSource();
			replace1.set("regex", "B1");
			replace1.set("to", "B${row}");
			parser.set("formula_replace", Arrays.asList(replace0, replace1));

			parser.addColumn("text", "string");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(2));
			assertThat(result.get(0).getAsString("text"), is("test3-a1"));
			assertThat(result.get(1).getAsString("text"), is("test2-b2"));
		}
	}

	@Test
	public void testSearchMergedCell_true() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "merged_cell");
			parser.addColumn("a", "string");
			parser.addColumn("b", "string");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(4));
			check6(result, 0, "test3-a1", "test3-a1");
			check6(result, 1, "data", "0");
			check6(result, 2, null, null);
			check6(result, 3, null, null);
		}
	}

	@Test
	public void testSearchMergedCell_false() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "merged_cell");
			parser.set("search_merged_cell", false);
			parser.addColumn("a", "string");
			parser.addColumn("b", "string");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(4));
			check6(result, 0, "test3-a1", null);
			check6(result, 1, "data", "0");
			check6(result, 2, null, null);
			check6(result, 3, null, null);
		}
	}

	private void check6(List<OutputRecord> result, int index, String a, String b) {
		OutputRecord r = result.get(index);
		// System.out.println(r);
		assertThat(r.getAsString("a"), is(a));
		assertThat(r.getAsString("b"), is(b));
	}

	@Test
	public void testStyle() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "style");
			parser.addColumn("color-text", "string");
			parser.addColumn("color", "string").set("value_type", "cell_style")
					.set("cell_style_name", "fillForegroundColor");
			parser.addColumn("border-text", "string");
			parser.addColumn("border-top", "long").set("cell_style_name", "borderTop");
			parser.addColumn("border-bottom", "long").set("cell_style_name", "borderBottom");
			parser.addColumn("border-left", "long").set("cell_style_name", "borderLeft");
			parser.addColumn("border-right", "long").set("cell_style_name", "borderRight");
			parser.addColumn("border-all", "long").set("cell_style_name", "border");
			parser.addColumn("font-color", "long").set("column_number", "C").set("cell_style_name", "fontColor");
			parser.addColumn("font-bold", "boolean").set("cell_style_name", "fontBold");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(5));
			check7(result, 0, "red", 255, 0, 0, "top", CellStyle.BORDER_THIN, 0, 0, 0, null, false);
			check7(result, 1, "green", 0, 128, 0, null, 0, 0, 0, 0, 0xff0000L, true);
			check7(result, 2, "blue", 0, 0, 255, "left", 0, 0, CellStyle.BORDER_THIN, 0, null, null);
			check7(result, 3, "white", 255, 255, 255, "right", 0, 0, 0, CellStyle.BORDER_THIN, null, null);
			check7(result, 4, "black", 0, 0, 0, "bottom", 0, CellStyle.BORDER_MEDIUM, 0, 0, null, null);
		}
	}

	private void check7(List<OutputRecord> result, int index, String colorText, int r, int g, int b, String borderText,
			long top, long bottom, long left, long right, Long fontColor, Boolean fontBold) {
		OutputRecord record = result.get(index);
		// System.out.println(record);
		assertThat(record.getAsString("color-text"), is(colorText));
		assertThat(record.getAsString("color"), is(String.format("%02x%02x%02x", r, g, b)));
		assertThat(record.getAsString("border-text"), is(borderText));
		assertThat(record.getAsLong("border-top"), is(top));
		assertThat(record.getAsLong("border-bottom"), is(bottom));
		assertThat(record.getAsLong("border-left"), is(left));
		assertThat(record.getAsLong("border-right"), is(right));
		assertThat(record.getAsLong("border-all"), is(top << 24 | bottom << 16 | left << 8 | right));
		assertThat(record.getAsLong("font-color"), is(fontColor));
		assertThat(record.getAsBoolean("font-bold"), is(fontBold));
	}

	@Test
	public void testComment() throws ParseException {
		try (EmbulkPluginTester tester = new EmbulkPluginTester()) {
			tester.addParserPlugin(PoiExcelParserPlugin.TYPE, PoiExcelParserPlugin.class);

			EmbulkTestParserConfig parser = tester.newParserConfig(PoiExcelParserPlugin.TYPE);
			parser.set("sheet", "comment");
			parser.addColumn("comment", "string").set("value_type", "cell_comment");

			URL inFile = getClass().getResource("test1.xls");
			List<OutputRecord> result = tester.runParser(inFile, parser);

			assertThat(result.size(), is(2));
			assertThat(result.get(0).getAsString("comment"), is("hishidama:\nコメント"));
			assertThat(result.get(1).getAsString("comment"), is(nullValue()));
		}
	}
}