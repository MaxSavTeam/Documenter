package com.maxsavitsky.documenter.data.html;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.view.Display;
import android.view.View;
import android.widget.Button;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.utils.exceptions.WrongCssParamException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class HtmlSpanRender {
	private static final String TAG = MainActivity.TAG + " HtmlRender";

	private HtmlSpanRender() {}

	public static class Initialization {
		private boolean openImages = true;
		private String source;
		private final RenderCallback mRenderCallback;
		private final Html.ImageGetter mImageGetter = new HtmlImageLoader();
		private final Activity mActivity;

		public Initialization(@NonNull Activity context, @NonNull RenderCallback renderCallback) {
			mRenderCallback = renderCallback;
			mActivity = context;
		}

		public Context getViewsContext() {
			return mActivity;
		}

		public Activity getActivity() {
			return mActivity;
		}

		public boolean isOpenImages() {
			return openImages;
		}

		public Initialization setOpenImages(boolean openImages) {
			this.openImages = openImages;
			return this;
		}

		public String getSource() {
			return source;
		}

		public Initialization setSource(String source) {
			this.source = source;
			return this;
		}

		public RenderCallback getRenderCallback() {
			return mRenderCallback;
		}

		public Html.ImageGetter getImageGetter() {
			return mImageGetter;
		}
	}

	public static Spanned get(Initialization initialization) throws IOException, SAXException, ParserConfigurationException {
		String source = initialization.getSource();
		Document doc = Jsoup.parse( source );
		Elements elements = doc.select( "div[align]" );
		for (Element element : elements) {
			String styleAttr = element.attr( "style" );
			element.attr( "style", styleAttr + "text-align:" + element.attr( "align" ) + ";" );
			element.removeAttr( "align" );
		}
		elements.select( "img" ).after( "<br>" );

		doc.outputSettings().syntax( Document.OutputSettings.Syntax.xml ).prettyPrint( false );
		source = Parser.unescapeEntities( doc.html(), false ).replaceAll( ";=\"\"", "" );
		initialization.setSource( source );

		TagHandler tagHandler = new TagHandler( initialization );
		SAXParserFactory.newInstance().newSAXParser()
				.parse( new ByteArrayInputStream( source.getBytes( StandardCharsets.UTF_8 ) ),
						tagHandler );
		return tagHandler.getSpanned();
	}

	public interface RenderCallback {
		void onImageClick(View view, String src);

		@IntRange(from = 1)
		int getLineHeight();

		default boolean drawView(View view) {return false;}

		default void viewsArrayDone(ArrayList<WidgetParam> widgetParams){}
	}

	public static class WidgetParam{
		public final int firstAffectedLine, lastAffectedLine;
		public final View view;

		public WidgetParam(int from, int to, View view) {
			this.firstAffectedLine = from;
			this.lastAffectedLine = to;
			this.view = view;
		}
	}
}

class TagHandler extends DefaultHandler {
	private static final String TAG = MainActivity.TAG + " TagHandler";

	private final Stack<StackElement> mStack;
	private int currentTag = 0;
	private final SpannableStringBuilder mSpannableStringBuilder;
	private final Html.ImageGetter mImageGetter;
	private final HtmlSpanRender.RenderCallback mRenderCallback;

	private final HtmlSpanRender.Initialization mInitialization;

	public TagHandler(HtmlSpanRender.Initialization initialization) {
		mSpannableStringBuilder = new SpannableStringBuilder();
		mStack = new Stack<>();
		mInitialization = initialization;
		mImageGetter = mInitialization.getImageGetter();
		mRenderCallback = mInitialization.getRenderCallback();
	}

	private int len() {
		return mSpannableStringBuilder.length();
	}

	public Spanned getSpanned() {
		return mSpannableStringBuilder;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if ( isIgnorable( qName ) ) {
			return;
		}
		if ( isSingle( qName ) ) {
			processSingleElement( qName, attributes );
		} else {
			processBlockElement( qName, attributes );
		}
	}

	private static class SetElement {
		final int line;
		final int drawableHeight;

		public SetElement(int line, int drawableHeight) {
			this.line = line;
			this.drawableHeight = drawableHeight;
		}
	}

	private final Set<SetElement> mImagesSet = new HashSet<>();
	private int linesCount = 0;
	private final ArrayList<HtmlSpanRender.WidgetParam> mWidgetParams = new ArrayList<>();

	private void processSingleElement(String element, Attributes attributes) {
		if ( element.equals( "img" ) ) {
			String src = attributes.getValue( "src" );
			/*if(len() == 0 || mSpannableStringBuilder.charAt( len()-1 ) != '\n')
				mSpannableStringBuilder.append( "\n" );*/
			mSpannableStringBuilder.append( "\uFFFC" );
			linesCount++;
			Drawable drawable = mImageGetter.getDrawable( src );
			mImagesSet.add( new SetElement( linesCount, drawable.getBounds().bottom ) );
			mSpannableStringBuilder.setSpan( new ImageSpan( drawable, src ), len() - 1, len(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			//mImagesSet.add( new SetElement( mSpannableStringBuilder ) )
			if ( mRenderCallback != null ) {
				mSpannableStringBuilder.setSpan( new ClickableSpan() {
					@Override
					public void onClick(@NonNull View widget) {
						widget.setTransitionName( "html_image" );
						mRenderCallback.onImageClick( widget, src );
					}
				}, len() - 1, len(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}
		} else if ( element.equals( "br" ) ) {
			mSpannableStringBuilder.append( "\n" );
			linesCount++;
		} else if ( element.equals( "button" ) ) {
			Button button = new Button( mInitialization.getViewsContext() );
			button.setText( attributes.getValue( "text" ) );

			drawView( button );
		}
	}

	private void drawView(View view){
		Display display = mInitialization.getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize( size );
		view.measure( size.x, size.y );
		int measureHeight = view.getMeasuredHeight();

		int lineHeight = mRenderCallback.getLineHeight();
		int simpleLinesCount = linesCount;
		int imagesLinesHeight = 0;
		for (SetElement setElement : mImagesSet) {
			if ( setElement.line > linesCount ) {
				break;
			}
			imagesLinesHeight += setElement.drawableHeight;
			simpleLinesCount--;
		}
		view.setY( simpleLinesCount * lineHeight + imagesLinesHeight + 20 );

		if ( mRenderCallback.drawView( view ) ) {
			int newLines = measureHeight / lineHeight;
			if ( newLines * lineHeight < measureHeight ) {
				newLines++;
			}
			mWidgetParams.add( new HtmlSpanRender.WidgetParam( linesCount, linesCount + newLines, view ) );
			linesCount += newLines;
			for (int i = 0; i < newLines; i++)
				mSpannableStringBuilder.append( "\n" );
		}
	}

	private void processBlockElement(String element, Attributes attributes) {
		mStack.push( new StackElement( element, ++currentTag ) );
		if ( isStyleable( element ) ) {
			String style = attributes.getValue( "style" );
			if ( style != null ) {
				processCss( style );
			}
		} else {
			processNonStyleable( element );
		}
	}

	private void processNonStyleable(String element) {
		if ( element.equals( "sup" ) ) {
			addSpanToEnd( new SuperscriptSpan() );
		} else if ( element.equals( "sub" ) ) {
			addSpanToEnd( new SubscriptSpan() );
		}
	}

	private boolean isStyleable(String element) {
		ArrayList<String> nonStyleable = new ArrayList<>();
		nonStyleable.add( "sub" );
		nonStyleable.add( "sup" );
		for (String s : nonStyleable)
			if ( s.equals( element ) ) {
				return false;
			}
		return true;
	}

	private boolean isSingle(String element) {
		return element.equals( "img" ) || element.equals( "br" ) || element.equals( "button" );
	}

	private boolean isIgnorable(String element) {
		ArrayList<String> elements = new ArrayList<>();
		elements.add( "html" );
		elements.add( "head" );
		elements.add( "body" );
		elements.add( "footer" );
		elements.add( "header" );
		for (String s : elements)
			if ( s.equals( element ) ) {
				return true;
			}
		return false;
	}

	private final Map<Integer, ArrayList<Object>> mSpansMap = new HashMap<>();

	private void addSpanToEnd(Object span) {
		ArrayList<Object> spans = mSpansMap.get( currentTag );
		if ( spans == null ) {
			mSpansMap.put( currentTag, new ArrayList<Object>() {{
				add( span );
			}} );
		} else {
			spans.add( span );
		}
		( (Editable) mSpannableStringBuilder ).setSpan( span, mSpannableStringBuilder.length(), mSpannableStringBuilder.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE );
	}

	private void processCss(String styleAttr) {
		for (String s : styleAttr.split( ";" )) {
			String property = s.split( ":" )[ 0 ];
			String value = s.split( ":" )[ 1 ];
			switch ( property ) {
				case "font-size":
					Pair<Double, String> p = extractNumber( value );
					if ( p.first == null || p.second == null ) {
						continue;
					}
					if ( "px".equals( p.second ) ) {
						addSpanToEnd( new AbsoluteSizeSpan( p.first.intValue(), true ) );
					} else if ( "em".equals( p.second ) ) {
						addSpanToEnd( new RelativeSizeSpan( p.first.floatValue() ) );
					}
					break;
				case "background-color":
				case "color":
					if ( property.equals( "background-color" ) ) {
						addSpanToEnd( new BackgroundColorSpan( Color.parseColor( value ) ) );
					} else {
						addSpanToEnd( new ForegroundColorSpan( Color.parseColor( value ) ) );
					}
					break;
				case "text-align":
					Layout.Alignment alignment = null;
					if ( value.equals( "center" ) ) {
						alignment = Layout.Alignment.ALIGN_CENTER;
					} else if ( value.equals( "right" ) ) {
						alignment = Layout.Alignment.ALIGN_OPPOSITE;
					}
					if ( alignment != null ) {
						addSpanToEnd( new AlignmentSpan.Standard( alignment ) );
					}
					break;
			}
		}
	}

	private Pair<Double, String> extractNumber(String value) {
		int i = 0;
		String num = "";
		char ch = value.charAt( i );
		for (; Character.isDigit( ch ) || ch == ',' || ch == '.'; ch = value.charAt( ++i )) {
			if ( ch == ',' ) {
				ch = '.';
			}
			num = String.format( "%s%c", num, ch );
		}
		String designation = "";
		for (; i < value.length(); i++)
			designation = String.format( "%s%c", designation, value.charAt( i ) );
		double d;
		try {
			d = Double.parseDouble( num );
		} catch (NumberFormatException e) {
			throw new WrongCssParamException( "number format failed: " + e.getMessage() );
		}
		if ( designation.equals( "px" ) || designation.equals( "em" ) ) {
			return new Pair<>( d, designation );
		} else {
			throw new WrongCssParamException( "invalid designation: " + designation );
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		if ( !mStack.empty() ) {
			mSpannableStringBuilder.append( new String( ch, start, length ).replaceAll( "\n", "" ) );
		}
	}

	private void closeElement(int tag) {
		ArrayList<Object> spans = mSpansMap.get( tag );
		if ( spans != null ) {
			for (Object span : spans) {
				int start = mSpannableStringBuilder.getSpanStart( span );
				int end = mSpannableStringBuilder.length();
				//Log.i( TAG, "closeElement: start=" + start + " end=" + end );
				mSpannableStringBuilder.removeSpan( span );
				if ( start != -1 && start != end ) {
					//Log.i( TAG, "closeElement: closing " + span.getClass().getName() );
					mSpannableStringBuilder.setSpan( span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if ( isIgnorable( qName ) || isSingle( qName ) ) {
			return;
		}
		int elementTag = mStack.peek().tag;
		mStack.pop();
		closeElement( elementTag );
	}

	@Override
	public void endDocument() {
		mRenderCallback.viewsArrayDone( mWidgetParams );
	}
}

class StackElement {
	final String element;
	final int tag;

	public StackElement(String element, int tag) {
		this.element = element;
		this.tag = tag;
	}
}
