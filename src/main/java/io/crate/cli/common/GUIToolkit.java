package io.crate.cli.common;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.*;
import java.awt.*;
import java.sql.Types;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class GUIToolkit {

    static {
        // antialiased fonts
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
    }

    public static final String LOGO_FILE_NAME = "/cratedb_logo.png";
    public static final Color CRATE_COLOR = new Color(66, 188, 245);
    public static final int FRAME_WIDTH_AS_PERCENT_OF_SCREEN_WIDTH = 90;
    public static final int FRAME_HEIGHT_AS_PERCENT_OF_SCREEN_WIDTH = 90;

    public static final int NUM_COMMAND_BOARDS = 6;


    public static String toCommandBoardKey(int offset) {
        if (offset < 0 || offset >= NUM_COMMAND_BOARDS) {
            throw new IllegalArgumentException(String.format(
                    Locale.ENGLISH,
                    "offset: %d out of range (max: %d)",
                    offset,
                    NUM_COMMAND_BOARDS - 1));
        }
        return String.valueOf((char) ('A' + offset));
    }

    public static int fromCommandBoardKey(String key) {
        if (null == key || key.trim().length() < 1) {
            throw new IllegalArgumentException(String.format(
                    Locale.ENGLISH,
                    "key cannot be null, must contain one non white char: %s",
                    key));
        }
        int offset = key.trim().charAt(0) - 'A';
        if (offset < 0 || offset >= NUM_COMMAND_BOARDS) {
            throw new IndexOutOfBoundsException(String.format(
                    Locale.ENGLISH,
                    "Key [%s] -> offset: %d (max: %d)",
                    key, offset, NUM_COMMAND_BOARDS - 1));
        }
        return offset;
    }

    public static final String JDBC_DRIVER_URL_FORMAT = "jdbc:crate://%s:%s/";

    public static final String MAIN_FONT_NAME = "monospaced";
    public static final Dimension SQL_CONNECTION_MANAGER_HEIGHT = new Dimension(0, 200);
    public static final Dimension COMMAND_BOARD_MANAGER_HEIGHT = new Dimension(0, 300);
    public static final String COMMAND_BOARD_MANAGER_STORE = "command_board.json";
    public static final String SQL_CONNECTION_MANAGER_STORE = "connections.json";

    public static final String ERROR_HEADER = "======= Error =======\n";
    public static final Font ERROR_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 14);
    public static final Color ERROR_FONT_COLOR = new Color(189, 4, 4); // blood

    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public static final Color TABLE_CELL_COLOR = CRATE_COLOR;
    public static final Color TABLE_FOOTER_COLOR = Color.BLACK;
    public static final Font TABLE_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Font TABLE_CELL_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 16);
    public static final Font TABLE_FOOTER_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 14);
    public static final int TABLE_ROW_HEIGHT = 20;

    public static final Color TABLE_BG_ROW_COLOR = new Color(230, 236, 255);
    public static final Color TABLE_BG_ROW_COLOR_ALTERNATE = new Color(255, 247, 255);
    public static final Color SELECTED_BG_COLOR = new Color(102, 140, 255);
    public static final Color SELECTED_FG_COLOR = Color.WHITE;
    public static final Font TABLE_CELL_SELECTED_FONT = new Font(GUIToolkit.MAIN_FONT_NAME, Font.BOLD, 16);

    public static final Color COMMAND_BOARD_FONT_COLOR = Color.WHITE;
    public static final Color COMMAND_BOARD_KEYWORD_FONT_COLOR = CRATE_COLOR;
    public static final Color COMMAND_BOARD_BACKGROUND_COLOR = Color.BLACK;
    public static final Color COMMAND_BOARD_CARET_COLOR = Color.GREEN;
    public static final Font COMMAND_BOARD_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 16);
    public static final Font COMMAND_BOARD_BODY_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);

    public static final Border COMMAND_BOARD_CONNECTED_BORDER = BorderFactory.createLineBorder(CRATE_COLOR, 4, true);
    public static final Border COMMAND_BOARD_DISCONNECTED_BORDER = BorderFactory.createLineBorder(Color.BLACK, 2, true);
    public static final Border COMMAND_BOARD_UNSELECTED_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, false);

    public static String resolveNameForSqlType(int sqlType) {
        String type = null;
        switch (sqlType) {
            case Types.BOOLEAN:
                type = "BOOL";
                break;

            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                type = "INT";
                break;

            case Types.REAL:
            case Types.DOUBLE:
                type = "FLT";
                break;

            case Types.VARCHAR:
                type = "STR";
                break;

            default:
                System.out.println("No type for java.sql.Types type: " + sqlType);
        }
        return type;
    }

    public static Color resolveColorForSqlType(int sqlType) {
        Color color = Color.BLACK;
        switch (sqlType) {
            case Types.BOOLEAN:
                color = new Color(0, 0, 128); // navy
                break;

            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                color = new Color(128, 128, 0); // olive
                break;

            case Types.REAL:
            case Types.DOUBLE:
                color = Color.GREEN;
                break;

            case Types.VARCHAR:
                color = new Color(0, 128, 128);
                break;

            default:
                System.out.println("No color for java.sql.Types type: " + sqlType);
        }
        return color;
    }

    public static void addToSwingEventQueue(Runnable ... runnables) {
        if (EventQueue.isDispatchThread()) {
            for (Runnable r : runnables) {
                if (null != r) {
                    r.run();
                }
            }
        } else {
            try {
                EventQueue.invokeLater(() -> {
                    for (Runnable r : runnables) {
                        if (null != r) {
                            r.run();
                        }
                    }
                });
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    public static JFrame newFrame(String title,
                                  int screenWidthPercent,
                                  int screenHeightPercent,
                                  JPanel mainPanel) {
        if (screenWidthPercent <= 0 || screenWidthPercent > 100) {
            throw new IllegalArgumentException("screenWidthPercent must be a value in [1, 100]");
        }
        if (screenHeightPercent <= 0 || screenHeightPercent > 100) {
            throw new IllegalArgumentException("screenHeightPercent must be a value in [1, 100]");
        }

        JFrame frame = new JFrame();
        frame.setTitle(title);
        frame.setType(Window.Type.NORMAL);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(mainPanel, BorderLayout.CENTER);
        ImageIcon logo = new ImageIcon(GUIToolkit.class.getResource(LOGO_FILE_NAME));
        frame.setIconImage(logo.getImage());

        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int width = (int) (screenSize.getWidth() * screenWidthPercent / 100);
        int height = (int) (screenSize.getHeight() * screenHeightPercent / 100);
        int x = (int) (screenSize.getWidth() - width) / 2;
        int y = (int) (screenSize.getHeight() - height) / 2;
        frame.setSize(width, height);
        frame.setLocation(x, y);
        return frame;
    }

    public static JTable newTable(TableModel tableModel, Runnable onListSelection) {
        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setGridColor(TABLE_CELL_COLOR);
        table.setFont(TABLE_CELL_FONT);
        table.setDefaultRenderer(String.class, new StringCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (false == e.getValueIsAdjusting()) {
                if (null != onListSelection) {
                    onListSelection.run();
                }
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setFont(TABLE_HEADER_FONT);
        header.setForeground(TABLE_HEADER_FONT_COLOR);
        TableColumnModel columnModel = header.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);
        return table;
    }

    public static  JTextPane newTextComponent() {
        JTextPane textPane = new JTextPane();
        textPane.setCaretPosition(0);
        textPane.setMargin(new Insets(5, 5, 5, 5));
        textPane.setFont(COMMAND_BOARD_BODY_FONT);
        textPane.setForeground(COMMAND_BOARD_FONT_COLOR);
        textPane.setBackground(COMMAND_BOARD_BACKGROUND_COLOR);
        textPane.setCaretColor(COMMAND_BOARD_CARET_COLOR);
        ((AbstractDocument) textPane.getDocument()).setDocumentFilter(createDocumentFilter(textPane));
        return textPane;
    }

    private final static StyleContext styleContext = StyleContext.getDefaultStyleContext();
    private final static AttributeSet errorAttributeSet = styleContext.addAttribute(
            styleContext.getEmptySet(),
            StyleConstants.Foreground,
            ERROR_FONT_COLOR);
    private final static AttributeSet normalAttributeSet = styleContext.addAttribute(
            styleContext.getEmptySet(),
            StyleConstants.Foreground,
            COMMAND_BOARD_FONT_COLOR);
    private final static AttributeSet keywordAttributeSet = styleContext.addAttribute(
            styleContext.getEmptySet(),
            StyleConstants.Foreground,
            COMMAND_BOARD_KEYWORD_FONT_COLOR);

    private static DocumentFilter createDocumentFilter(JTextPane textPane) {
        return new DocumentFilter() {

            private final StyledDocument styledDocument = textPane.getStyledDocument();

            private void handleTextChanged() throws BadLocationException {
                int len = styledDocument.getLength();
                String text = styledDocument.getText(0, len);
                boolean isError = text.contains(ERROR_HEADER);
                if (isError) {
                    styledDocument.setCharacterAttributes(0, len, errorAttributeSet, true);
                } else {
                    styledDocument.setCharacterAttributes(0, len, normalAttributeSet, true);
                    Matcher matcher = KEYWORDS_PATTERN.matcher(text);
                    while (matcher.find()) {
                        styledDocument.setCharacterAttributes(
                                matcher.start(),
                                matcher.end() - matcher.start(),
                                keywordAttributeSet,
                                false);
                    }
                }
            }

            @Override
            public void insertString(FilterBypass fb,
                                     int offset,
                                     String text,
                                     AttributeSet attributeSet) throws BadLocationException {
                super.insertString(fb, offset, text, attributeSet);
                handleTextChanged();
            }

            @Override
            public void remove(FilterBypass fb,
                               int offset,
                               int length) throws BadLocationException {
                super.remove(fb, offset, length);
                handleTextChanged();
            }

            @Override
            public void replace(FilterBypass fb,
                                int offset,
                                int length,
                                String text,
                                AttributeSet attributeSet) throws BadLocationException {
                super.replace(fb, offset, length, text, attributeSet);
                handleTextChanged();
            }
        };
    }

    private GUIToolkit() {
        throw new IllegalStateException("not meant to me instantiated");
    }

    private static final Pattern KEYWORDS_PATTERN = Pattern.compile(
            "\\bcrate\\b|\\babs\\b|\\babsolute\\b|\\baction\\b|\\badd\\b|\\bafter\\b|\\balias\\b"
                    + "|\\ball\\b|\\ballocate\\b|\\balter\\b|\\balways\\b|\\banalyze\\b|\\banalyzer\\b"
                    + "|\\band\\b|\\bany\\b|\\bare\\b|\\barray\\b|\\barray_agg\\b|\\barray_max_cardinality\\b"
                    + "|\\bartifacts\\b|\\bas\\b|\\basc\\b|\\basensitive\\b|\\bassertion\\b|\\basterisk\\b"
                    + "|\\basymmetric\\b|\\bat\\b|\\batomic\\b|\\bauthorization\\b|\\bavg\\b|\\bbackquoted_identifier\\b"
                    + "|\\bbefore\\b|\\bbegin\\b|\\bbegin_frame\\b|\\bbegin_partition\\b|\\bbernoulli\\b"
                    + "|\\bbetween\\b|\\bbigint\\b|\\bbinary\\b|\\bbit\\b|\\bbit_length\\b|\\bblob\\b"
                    + "|\\bboolean\\b|\\bboth\\b|\\bbreadth\\b|\\bby\\b|\\bbyte\\b|\\bcall\\b|\\bcalled\\b"
                    + "|\\bcancel\\b|\\bcardinality\\b|\\bcascade\\b|\\bcascaded\\b|\\bcase\\b"
                    + "|\\bcast\\b|\\bcast_operator\\b|\\bcatalog\\b|\\bcatalogs\\b|\\bceil\\b"
                    + "|\\bceiling\\b|\\bchar\\b|\\bchar_filters\\b|\\bchar_length\\b|\\bcharacter\\b"
                    + "|\\bcharacter_length\\b|\\bcharacteristics\\b|\\bcheck\\b|\\bclob\\b"
                    + "|\\bclose\\b|\\bcluster\\b|\\bclustered\\b|\\bcoalesce\\b|\\bcollate\\b"
                    + "|\\bcollation\\b|\\bcollect\\b|\\bcolon_ident\\b|\\bcolumn\\b|\\bcolumns\\b"
                    + "|\\bcomment\\b|\\bcommit\\b|\\bcommitted\\b|\\bconcat\\b|\\bcondition\\b"
                    + "|\\bconflict\\b|\\bconnect\\b|\\bconnection\\b|\\bconstraint\\b|\\bconstraints\\b"
                    + "|\\bconstructor\\b|\\bcontains\\b|\\bcontinue\\b|\\bconvert\\b|\\bcopy\\b"
                    + "|\\bcorr\\b|\\bcorresponding\\b|\\bcount\\b|\\bcovar_pop\\b|\\bcovar_samp\\b"
                    + "|\\bcreate\\b|\\bcross\\b|\\bcube\\b|\\bcume_dist\\b|\\bcurrent\\b|\\bcurrent_catalog\\b"
                    + "|\\bcurrent_date\\b|\\bcurrent_path\\b|\\bcurrent_role\\b|\\bcurrent_row\\b"
                    + "|\\bcurrent_schema\\b|\\bcurrent_time\\b|\\bcurrent_timestamp\\b|\\bcurrent_user\\b"
                    + "|\\bcursor\\b|\\bcycle\\b|\\bdangling\\b|\\bdata\\b|\\bdate\\b|\\bday\\b"
                    + "|\\bdeallocate\\b|\\bdec\\b|\\bdecimal\\b|\\bdecimal_value\\b|\\bdeclare\\b"
                    + "|\\bdecommission\\b|\\bdefault\\b|\\bdeferrable\\b|\\bdeferred\\b|\\bdelete\\b"
                    + "|\\bdense_rank\\b|\\bdeny\\b|\\bdepth\\b|\\bderef\\b|\\bdesc\\b|\\bdescribe\\b"
                    + "|\\bdescriptor\\b|\\bdeterministic\\b|\\bdiagnostics\\b|\\bdigit_identifier\\b"
                    + "|\\bdirectory\\b|\\bdisconnect\\b|\\bdistinct\\b|\\bdistributed\\b|\\bdo\\b"
                    + "|\\bdomain\\b|\\bdouble\\b|\\bdrop\\b|\\bduplicate\\b|\\bdynamic\\b|\\beach\\b"
                    + "|\\belement\\b|\\belse\\b|\\belseif\\b|\\bend\\b|\\bend_exec\\b|\\bend_frame\\b"
                    + "|\\bend_partition\\b|\\beq\\b|\\bequals\\b|\\bescape\\b|\\bescaped_string\\b"
                    + "|\\bevery\\b|\\bexcept\\b|\\bexception\\b|\\bexec\\b|\\bexecute\\b|\\bexists\\b"
                    + "|\\bexit\\b|\\bexplain\\b|\\bextends\\b|\\bexternal\\b|\\bextract\\b|\\bfailed\\b"
                    + "|\\bfalse\\b|\\bfetch\\b|\\bfilter\\b|\\bfirst\\b|\\bfirst_value\\b|\\bfloat\\b"
                    + "|\\bfollowing\\b|\\bfor\\b|\\bforeign\\b|\\bformat\\b|\\bfound\\b|\\bframe_row\\b"
                    + "|\\bfree\\b|\\bfrom\\b|\\bfull\\b|\\bfulltext\\b|\\bfunction\\b|\\bfunctions\\b"
                    + "|\\bfusion\\b|\\bgc\\b|\\bgeneral\\b|\\bgenerated\\b|\\bgeo_point\\b|\\bgeo_shape\\b"
                    + "|\\bget\\b|\\bglobal\\b|\\bgo\\b|\\bgoto\\b|\\bgrant\\b|\\bgraphviz\\b|\\bgroup\\b"
                    + "|\\bgrouping\\b|\\bgroups\\b|\\bgt\\b|\\bgte\\b|\\bhandler\\b|\\bhaving\\b"
                    + "|\\bhold\\b|\\bhour\\b|\\bidentifier\\b|\\bidentity\\b|\\bif\\b|\\bignored\\b"
                    + "|\\bilike\\b|\\bimmediate\\b|\\bin\\b|\\bindex\\b|\\bindicator\\b|\\binitially\\b"
                    + "|\\binner\\b|\\binout\\b|\\binput\\b|\\binsensitive\\b|\\binsert\\b|\\bint\\b"
                    + "|\\binteger\\b|\\binteger_value\\b|\\bintersect\\b|\\bintersection\\b"
                    + "|\\binterval\\b|\\binto\\b|\\bip\\b|\\bis\\b|\\bisolation\\b|\\biterate\\b"
                    + "|\\bjoin\\b|\\bkey\\b|\\bkill\\b|\\blanguage\\b|\\blarge\\b|\\blast\\b|\\blast_value\\b"
                    + "|\\blateral\\b|\\blead\\b|\\bleading\\b|\\bleave\\b|\\bleft\\b|\\blevel\\b"
                    + "|\\blicense\\b|\\blike\\b|\\blike_regex\\b|\\blimit\\b|\\bllt\\b|\\bln\\b"
                    + "|\\blocal\\b|\\blocaltime\\b|\\blocaltimestamp\\b|\\blocator\\b|\\blogical\\b"
                    + "|\\blong\\b|\\bloop\\b|\\blower\\b|\\blt\\b|\\blte\\b|\\bmap\\b|\\bmatch\\b"
                    + "|\\bmaterialized\\b|\\bmax\\b|\\bmember\\b|\\bmerge\\b|\\bmethod\\b|\\bmin\\b"
                    + "|\\bminus\\b|\\bminute\\b|\\bmod\\b|\\bmodifies\\b|\\bmodule\\b|\\bmonth\\b"
                    + "|\\bmove\\b|\\bmultiset\\b|\\bnames\\b|\\bnational\\b|\\bnatural\\b|\\bnchar\\b"
                    + "|\\bnclob\\b|\\bneq\\b|\\bnew\\b|\\bnext\\b|\\bno\\b|\\bnone\\b|\\bnormalize\\b"
                    + "|\\bnot\\b|\\bnothing\\b|\\bnth_value\\b|\\bntile\\b|\\bnull\\b|\\bnullif\\b"
                    + "|\\bnulls\\b|\\bnumeric\\b|\\bobject\\b|\\boctet_length\\b|\\bof\\b|\\boff\\b"
                    + "|\\boffset\\b|\\bold\\b|\\bon\\b|\\bonly\\b|\\bopen\\b|\\boptimize\\b|\\boption\\b"
                    + "|\\bor\\b|\\border\\b|\\bordinality\\b|\\bout\\b|\\bouter\\b|\\boutput\\b"
                    + "|\\bover\\b|\\boverlaps\\b|\\boverlay\\b|\\bpad\\b|\\bparameter\\b|\\bpartial\\b"
                    + "|\\bpartition\\b|\\bpartitioned\\b|\\bpartitions\\b|\\bpath\\b|\\bpercent\\b"
                    + "|\\bpercent_rank\\b|\\bpercentile_cont\\b|\\bpercentile_disc\\b|\\bperiod\\b"
                    + "|\\bpersistent\\b|\\bplain\\b|\\bplus\\b|\\bportion\\b|\\bposition\\b|\\bposition_regex\\b"
                    + "|\\bpower\\b|\\bprecedes\\b|\\bpreceding\\b|\\bprecision\\b|\\bprepare\\b"
                    + "|\\bpreserve\\b|\\bprimary\\b|\\bprimary key\\b|\\bprimary_key\\b|\\bprior\\b"
                    + "|\\bprivileges\\b|\\bprocedure\\b|\\bpromote\\b|\\bpublic\\b|\\bquoted_identifier\\b"
                    + "|\\brange\\b|\\brank\\b|\\bread\\b|\\breads\\b|\\breal\\b|\\brecursive\\b"
                    + "|\\bref\\b|\\breferences\\b|\\breferencing\\b|\\brefresh\\b|\\bregex_match\\b"
                    + "|\\bregex_match_ci\\b|\\bregex_no_match\\b|\\bregex_no_match_ci\\b"
                    + "|\\bregr_avgx\\b|\\bregr_avgy\\b|\\bregr_count\\b|\\bregr_intercept\\b"
                    + "|\\bregr_r2\\b|\\bregr_slope\\b|\\bregr_sxx\\b|\\bregr_sxyregr_syy\\b"
                    + "|\\brelative\\b|\\brelease\\b|\\brename\\b|\\brepeat\\b|\\brepeatable\\b"
                    + "|\\breplace\\b|\\breplica\\b|\\brepository\\b|\\breroute\\b|\\breset\\b"
                    + "|\\bresignal\\b|\\brestore\\b|\\brestrict\\b|\\bresult\\b|\\bretry\\b|\\breturn\\b"
                    + "|\\breturns\\b|\\brevoke\\b|\\bright\\b|\\brole\\b|\\brollback\\b|\\brollup\\b"
                    + "|\\broutine\\b|\\brow\\b|\\brow_number\\b|\\brows\\b|\\bsavepoint\\b|\\bschema\\b"
                    + "|\\bschemas\\b|\\bscope\\b|\\bscroll\\b|\\bsearch\\b|\\bsecond\\b|\\bsection\\b"
                    + "|\\bselect\\b|\\bsemicolon\\b|\\bsensitive\\b|\\bserializable\\b|\\bsession\\b"
                    + "|\\bsession_user\\b|\\bset\\b|\\bsets\\b|\\bshard\\b|\\bshards\\b|\\bshort\\b"
                    + "|\\bshow\\b|\\bsignal\\b|\\bsimilar\\b|\\bsize\\b|\\bslash\\b|\\bsmallint\\b"
                    + "|\\bsnapshot\\b|\\bsome\\b|\\bspace\\b|\\bspecific\\b|\\bspecifictype\\b"
                    + "|\\bsql\\b|\\bsqlcode\\b|\\bsqlerror\\b|\\bsqlexception\\b|\\bsqlstate\\b"
                    + "|\\bsqlwarning\\b|\\bsqrt\\b|\\bstart\\b|\\bstate\\b|\\bstatic\\b|\\bstddev_pop\\b"
                    + "|\\bstddev_samp\\b|\\bstorage\\b|\\bstratify\\b|\\bstrict\\b|\\bstring\\b"
                    + "|\\bstring_type\\b|\\bsubmultiset\\b|\\bsubstring\\b|\\bsubstring_regex\\b"
                    + "|\\bsucceedsblob\\b|\\bsum\\b|\\bsummary\\b|\\bswap\\b|\\bsymmetric\\b"
                    + "|\\bsystem\\b|\\bsystem_time\\b|\\bsystem_user\\b|\\btable\\b|\\btables\\b"
                    + "|\\btablesample\\b|\\btemporary\\b|\\btext\\b|\\bthen\\b|\\btime\\b|\\btimestamp\\b"
                    + "|\\btimezone_hour\\b|\\btimezone_minute\\b|\\bto\\b|\\btoken_filters\\b"
                    + "|\\btokenizer\\b|\\btrailing\\b|\\btransaction\\b|\\btransaction_isolation\\b"
                    + "|\\btransient\\b|\\btranslate\\b|\\btranslate_regex\\b|\\btranslation\\b"
                    + "|\\btreat\\b|\\btrigger\\b|\\btrim\\b|\\btrim_array\\b|\\btrue\\b|\\btruncate\\b"
                    + "|\\btry_cast\\b|\\btype\\b|\\buescape\\b|\\bunbounded\\b|\\buncommitted\\b"
                    + "|\\bunder\\b|\\bundo\\b|\\bunion\\b|\\bunique\\b|\\bunknown\\b|\\bunnest\\b"
                    + "|\\bunrecognized\\b|\\buntil\\b|\\bupdate\\b|\\bupper\\b|\\busage\\b|\\buser\\b"
                    + "|\\busing\\b|\\bvalue\\b|\\bvalue_of\\b|\\bvalues\\b|\\bvar_pop\\b|\\bvar_samp\\b"
                    + "|\\bvarbinary\\b|\\bvarchar\\b|\\bvarying\\b|\\bversioning\\b|\\bview\\b"
                    + "|\\bwhen\\b|\\bwhenever\\b|\\bwhere\\b|\\bwhile\\b|\\bwidth_bucket\\b|\\bwindow\\b"
                    + "|\\bwith\\b|\\bwithin\\b|\\bwithout\\b|\\bwork\\b|\\bwrite\\b|\\bws\\b|\\byear\\b"
                    + "|\\bzone\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
}
