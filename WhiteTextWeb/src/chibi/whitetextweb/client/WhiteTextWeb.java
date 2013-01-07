package chibi.whitetextweb.client;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chibi.whitetextweb.shared.DataGridRow;
import chibi.whitetextweb.shared.DataStatistics;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.ImageCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.view.client.ListDataProvider;

//TODO
//check - Caudal part of spinal trigeminal nucleus
/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WhiteTextWeb implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	DataGrid<DataGridRow> dataGrid;
	String regexForConnectionPreds;
	Label errorLabel;
	// SimplePager pager;

	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network " + "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side service.
	 */
	private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);

	/**
	 * This is the entry point method.
	 */

	public void onModuleLoad() {

		final Button sendButton = new Button("Search");
		// We can add style names to widgets
		sendButton.addStyleName("sendButton");

		// final TextBox nameField = new TextBox();
		// nameField.setText("GWT User");
		errorLabel = new Label();
		errorLabel.addStyleName("serverResponseLabelError");

		RootPanel.get("statsLabelContainer").add(new Label("Loading..."));

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element

		RootPanel.get("sendButtonContainer").add(sendButton);
		RootPanel.get("errorLabelContainer").add(errorLabel);

		final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
		final SuggestBox suggestBox = new SuggestBox(oracle);
		suggestBox.setWidth("300px");

		greetingService.getOracle(new AsyncCallback<Set<String>>() {
			public void onFailure(Throwable caught) {
				errorLabel.setText("Remote Procedure Call - Failure");
			}

			public void onSuccess(Set<String> result) {
				for (String resultString : result) {
					oracle.add(resultString);
				}
			}
		});

		RootPanel.get("suggestBoxContainer").add(suggestBox);
		// Focus the cursor on the name field when the app loads
		suggestBox.setFocus(true);

		InlineLabel citationLink = new InlineLabel("Cite");
		citationLink.addStyleName("citeLink");

		final DecoratedPopupPanel citaitonPopup = new DecoratedPopupPanel(true);
		citaitonPopup.setWidth("300px");
		String listing = "Please cite usage as: <br/><a target=\"blank\" href=\"http://bioinformatics.oxfordjournals.org/content/28/22/2963.long\">Application and evaluation of automated methods to extract neuroanatomical connectivity statements from free text. L French, S Lane, L Xu, C Siu, C Kwok, Y Chen, C Krebs and P Pavlidis. Bioinformatics, 2012</a>";
		listing += "<br/>Click on the text results for citations to the extracted sentences (PubMed/MEDLINE).";

		HTML html = new HTML(listing);

		html.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				citaitonPopup.hide();
			}
		});

		citaitonPopup.setWidget(html);

		citationLink.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				citaitonPopup.center();

			}
		});

		RootPanel.get("citationBoxContainer").add(citationLink);

		greetingService.getDataStatistics(new AsyncCallback<DataStatistics>() {
			public void onFailure(Throwable caught) {
				errorLabel.setText("Remote Procedure Call - Failure");
			}

			public void onSuccess(DataStatistics result) {
				Label statsLine = new Label("Indexing " + result.sentenceCount + " sentences with " + result.pairCount
						+ " connections between " + result.regionInstanceCount + " brain region mentions.");

				RootPanel statsContainer = RootPanel.get("statsLabelContainer");
				statsContainer.clear();
				statsContainer.add(statsLine);
				// regionCountLabel.setText( + "");
				// sentenceStatLabel.setText( + "");
				// connectionsStatLabel.setText( + "");
			}
		});
		// //////////////////////////////////////

		dataGrid = new DataGrid<DataGridRow>();

		dataGrid.setWidth("100%");

		final ListDataProvider<DataGridRow> dataProvider = new ListDataProvider<DataGridRow>();
		// List<DataGridRow> data = dataProvider.getList();
		// for (int i = 0; i < 50; i++) {
		// data.add(new DataGridRow("Item " + i, "" + Math.random(), "" +
		// Math.random()));
		// }

		// Set the message to display when the table is empty.
		dataGrid.setEmptyTableWidget(new Label("No data to display. Type, then select a brain region to search."));

		// we are not using pages, so set a large max page size
		dataGrid.setPageSize(10000);

		// Create a Pager to control the table.
		// SimplePager.Resources pagerResources =
		// GWT.create(SimplePager.Resources.class);
		// pager = new SimplePager(TextLocation.CENTER, pagerResources, false,
		// 0, true);
		// pager.setDisplay(dataGrid);
		final ListHandler<DataGridRow> sortHandler = new ListHandler<DataGridRow>(dataProvider.getList());
		dataGrid.addColumnSortHandler(sortHandler);

		// Initialize the columns.
		initColumns(sortHandler);

		// Add the CellList to the adapter in the database.
		// ContactDatabase.get().addDataDisplay(dataGrid);
		dataProvider.addDataDisplay(dataGrid);

		DockPanel dock = new DockPanel();
		RootPanel.get("dataGridContainer").setWidth("100%");
		RootPanel.get("dataGridContainer").add(dock);

		dock.setWidth("100%");

		dock.add(dataGrid, DockPanel.CENTER);

		setHeight();

		// we shouldn't need the scrollbar
		Window.enableScrolling(false);
		Window.addResizeHandler(new ResizeHandler() {
			public void onResize(ResizeEvent event) {
				setHeight();
			}
		});

		greetingService.getConnectionPredRegex(new AsyncCallback<String>() {
			public void onFailure(Throwable caught) {
				errorLabel.setText("Remote Procedure Call - Failure");
			}

			public void onSuccess(String result) {
				regexForConnectionPreds = result;
			}
		});

		// Create a handler for the sendButton and nameField
		class MyHandler implements ClickHandler, KeyUpHandler {
			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}

			/**
			 * Send the name from the nameField to the server and wait for a
			 * response.
			 */
			private void sendNameToServer() {
				errorLabel.setText("");
				String textToServer = suggestBox.getText();
				sendButton.setEnabled(false);

				greetingService.greetServer(textToServer, new AsyncCallback<List<DataGridRow>>() {
					public void onFailure(Throwable caught) {
						errorLabel.setText("Remote Procedure Call - Failure");
						sendButton.setEnabled(true);
					}

					public void onSuccess(List<DataGridRow> result) {
						// a setList is needed for sorthandler (due in new
						// version)
						// http://code.google.com/p/google-web-toolkit/issues/detail?id=7072
						errorLabel.setText("Received " + result.size() + " rows");

						int previousSize = dataGrid.getRowCount();
						dataProvider.getList().clear();
						dataProvider.getList().addAll(result);
						sortHandler.getList().clear();
						sortHandler.getList().addAll(result);
						dataGrid.addColumnSortHandler(sortHandler);

						if (!result.isEmpty() && previousSize > 0) {
							dataGrid.getRowElement(0).scrollIntoView();
						}
						sendButton.setEnabled(true);
					}
				});
			}
		}

		// Add a handler to send the name to the server
		MyHandler handler = new MyHandler();
		sendButton.addClickHandler(handler);
		suggestBox.addKeyUpHandler(handler);
	}

	private void setHeight() {
		int top = dataGrid.getAbsoluteTop();
		// errorLabel.setText(Window.getClientHeight() + " - " + top);
		String height = (Window.getClientHeight() - top - 5) + "px";
		RootPanel.get("dataGridContainer").setHeight(height);
		dataGrid.setSize("100%", height);
		dataGrid.setHeight(height);
		dataGrid.redraw();
	}

	private void initColumns(ListHandler<DataGridRow> sortHandler) {
		Column<DataGridRow, SafeHtml> sentenceCol = new Column<DataGridRow, SafeHtml>(new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(DataGridRow value) {
				String pmidURL = "http://www.ncbi.nlm.nih.gov/pubmed/" + value.pmid;

				String sentence = value.sentence;
				String match = value.entityOne;
				String connected = value.entityTwo;
				if (regexForConnectionPreds != null) {
					// text-decoration:underline
					// font-weight: bold;
					// color: #007A00
					sentence = sentence.replaceAll(regexForConnectionPreds,
							"<span STYLE=\"text-decoration:underline\">$1</span>");
				} else {
					// dependent on async call
					// errorLabel.setText("regexForConnectionPreds is Null");
				}
				// highlight the two regions
				// errorLabel.setText("MATCH TEXT:" + match);
				// sentence = sentence.replaceAll(match, "<span>XX" + match +
				// "XX</span>");

				String oldSentence = new String(sentence);
				try {
					sentence = sentence.replaceAll(match, "<span STYLE=\"font-weight:bold; color:#0101DF\">" + match
							+ "</span>");
				} catch (Exception e) {
					sentence = oldSentence;
					// "\\Q", "\\E" doesn't work in google GWT regex
					// skip over the ones we can't match
					// need Pattern.quote here or similar to escape brackets and
					// stuff in the brain regions
				}
				try {
					sentence = sentence.replaceAll(connected, "<span STYLE=\"font-weight:bold; color:#FF0040\">"
							+ connected + "</span>");
				} catch (Exception e) {
					sentence = oldSentence;
				}

				SafeHtml safeHTML;
				SafeHtmlBuilder sb = new SafeHtmlBuilder();
				sb.appendHtmlConstant("<a target=\"_blank\" STYLE=\"color:#000000\" href=\"" + pmidURL + "\">");
				try {
					sb.appendHtmlConstant(sentence);
					sb.appendHtmlConstant("</a>");
					safeHTML = sb.toSafeHtml();
				} catch (Exception e) {
					sb.appendHtmlConstant(oldSentence);
					sb.appendHtmlConstant("</a>");
					safeHTML = sb.toSafeHtml();
				}
				return safeHTML;
			}
		};

		// from org.gwtwidgets.client.util.regex.Pattern
		// TextColumn<DataGridRow> nameColumn = new TextColumn<DataGridRow>() {
		// @Override
		// public String getValue(DataGridRow object) {
		// return object.sentence;
		// }
		// };
		// dataGrid.addColumn(nameColumn, "Sentence");
		dataGrid.addColumn(sentenceCol, "Sentence");
		// dataGrid.addColumn(sentenceCol, null)

		TextColumn<DataGridRow> matchColumn = new TextColumn<DataGridRow>() {
			@Override
			public String getValue(DataGridRow object) {
				return object.entityOne;
			}
		};
		matchColumn.setSortable(true);
		dataGrid.setColumnWidth(matchColumn, 160, Unit.PX);
		sortHandler.setComparator(matchColumn, new Comparator<DataGridRow>() {
			public int compare(DataGridRow o1, DataGridRow o2) {
				return o1.entityOne.toLowerCase().compareTo(o2.entityOne.toLowerCase());
			}
		});
		matchColumn.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
		dataGrid.addColumn(matchColumn, "Query Region");

		TextColumn<DataGridRow> connectColumn = new TextColumn<DataGridRow>() {
			@Override
			public String getValue(DataGridRow object) {
				return object.entityTwo;
			}
		};
		connectColumn.setSortable(true);
		connectColumn.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
		dataGrid.setColumnWidth(connectColumn, 160, Unit.PX);

		sortHandler.setComparator(connectColumn, new Comparator<DataGridRow>() {
			public int compare(DataGridRow o1, DataGridRow o2) {
				return o1.entityTwo.toLowerCase().compareTo(o2.entityTwo.toLowerCase());
			}
		});
		dataGrid.addColumn(connectColumn, "Connected Region");

		TextColumn<DataGridRow> speciesColumn = new TextColumn<DataGridRow>() {
			@Override
			public String getValue(DataGridRow object) {
				return object.speciesLabel;
			}

			@Override
			public void render(Context context, DataGridRow value, SafeHtmlBuilder sb) {
				sb.appendHtmlConstant("<span title='species that are mentioned in the abstract'>");
				super.render(context, value, sb);
				sb.appendHtmlConstant("</span>");
			}
		};
		speciesColumn.setSortable(true);
		speciesColumn.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
		dataGrid.setColumnWidth(speciesColumn, 160, Unit.PX);

		sortHandler.setComparator(speciesColumn, new Comparator<DataGridRow>() {
			public int compare(DataGridRow o1, DataGridRow o2) {
				return o1.speciesLabel.compareTo(o2.speciesLabel);
			}
		});
		// dataGrid.addColumnStyleName(dataGrid.getColumnIndex(speciesColumn),
		// "center");

		SafeHtml safe = SafeHtmlUtils.fromSafeConstant("<center>Species</center>");
		dataGrid.addColumn(speciesColumn, safe);
		// dataGrid.addColumn(speciesColumn, "Species");
		// dataGrid.addColumn(speciesColumn, "Species");

		TextColumn<DataGridRow> scoreColumn = new TextColumn<DataGridRow>() {
			@Override
			public String getValue(DataGridRow object) {
				return NumberFormat.getFormat("0.00").format(object.score);
			}

			@Override
			public void render(Context context, DataGridRow value, SafeHtmlBuilder sb) {
				// tooltip
				sb.appendHtmlConstant("<span title='confidence that the author is stating connectivity between the two regions (higher is better)'>");
				super.render(context, value, sb);
				sb.appendHtmlConstant("</span>");
			}
		};
		scoreColumn.setSortable(true);
		scoreColumn.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
		dataGrid.setColumnWidth(scoreColumn, 80, Unit.PX);
		sortHandler.setComparator(scoreColumn, new Comparator<DataGridRow>() {
			public int compare(DataGridRow o1, DataGridRow o2) {
				return Double.compare(o1.score, o2.score);
			}
		});
		dataGrid.addColumn(scoreColumn, "Score");

		final ImageCell imageCell = new ImageCell() {
			public Set<String> getConsumedEvents() {
				Set<String> consumedEvents = new HashSet<String>();
				consumedEvents.add("click");
				return consumedEvents;
			}

			@Override
			public void render(Context context, String value, SafeHtmlBuilder sb) {
				sb.appendHtmlConstant("<span title='please flag if the two marked regions are not described as connected'>");
				super.render(context, value, sb);
				sb.appendHtmlConstant("</span>");
			}

			@Override
			public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
					ValueUpdater<String> valueUpdater) {
				switch (DOM.eventGetType((Event) event)) {
				case Event.ONCLICK:
					valueUpdater.update(value);
					// a bit ugly - we shouldn't need to redraw the whole table
					// using a AbstractEditable cell like a buttoncell may deal
					// with this better
					dataGrid.redraw();
					break;

				default:
					break;
				}
			}

		};
		Column<DataGridRow, String> flagColumn = new Column<DataGridRow, String>(imageCell) {
			public String getValue(DataGridRow object) {
				if (object.flagged == true) {
					return "images/flag_red.png";
				} else {
					return "images/flag_grey.png";
				}
			}
		};
		flagColumn.setFieldUpdater(new FieldUpdater<DataGridRow, String>() {
			public void update(int index, DataGridRow object, String value) {
				boolean flagged = !object.flagged;
				object.setChecked(flagged);

				String input = "PAIR=" + object.pairURI;
				input += "," + "BOXVALUE=" + flagged;

				greetingService.writeFlaggedColumn(input, new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						errorLabel.setText("Remote Procedure Call - Failure");
					}

					public void onSuccess(Void result) {
					}
				});
				errorLabel.setText("Thank you, we will review this extracted relation.");
			}
		});
		flagColumn.setSortable(true);
		sortHandler.setComparator(flagColumn, new Comparator<DataGridRow>() {
			public int compare(DataGridRow o1, DataGridRow o2) {
				return new Boolean(o1.flagged).compareTo(o2.flagged);
			}
		});

		dataGrid.setColumnWidth(flagColumn, 80, Unit.PX);
		dataGrid.addColumn(flagColumn, "Report");

	}

}
