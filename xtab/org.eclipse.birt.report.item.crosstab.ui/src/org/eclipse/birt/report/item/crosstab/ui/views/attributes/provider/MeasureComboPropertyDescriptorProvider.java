/**
 * 
 */

package org.eclipse.birt.report.item.crosstab.ui.views.attributes.provider;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.ComboPropertyDescriptorProvider;
import org.eclipse.birt.report.item.crosstab.core.de.CrosstabReportItemHandle;
import org.eclipse.birt.report.item.crosstab.internal.ui.AggregationCellProviderWrapper;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;

/**
 * @author Administrator
 * 
 */
public class MeasureComboPropertyDescriptorProvider extends
		ComboPropertyDescriptorProvider
{

	protected CrosstabReportItemHandle crosstabHandle;
	protected static Logger logger = Logger.getLogger( MeasureComboPropertyDescriptorProvider.class.getName( ) );

	public MeasureComboPropertyDescriptorProvider( String property,
			String element )
	{
		super( property, element );
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.IDescriptorProvider#save(java.lang.Object)
	 */
	public void save( Object value ) throws SemanticException
	{
		String stringValue = (String) value;
		if ( input == null )
		{
			return;
		}
		else if ( crosstabHandle == null )
		{
			initializeCrosstab( );
		}
		if ( stringValue != null )
		{
			crosstabHandle.setMeasureDirection( stringValue );
			AggregationCellProviderWrapper providerWrapper = new AggregationCellProviderWrapper(crosstabHandle);
			providerWrapper.updateAllAggregationCells( );
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.IDescriptorProvider#setInput(java.lang.Object)
	 */
	public void setInput( Object input )
	{
		// TODO Auto-generated method stub
		super.setInput( input );
		initializeCrosstab( );
	}

	protected void initializeCrosstab( )
	{
		crosstabHandle = null;
		if ( ( input == null ) )
		{
			return;
		}

		if ( ( !( input instanceof List && ( (List) input ).size( ) > 0 && ( (List) input ).get( 0 ) instanceof ExtendedItemHandle ) )
				&& ( !( input instanceof ExtendedItemHandle ) ) )
		{
			return;
		}

		ExtendedItemHandle handle;
		if ( ( (List) input ).size( ) > 0 )
		{
			handle = (ExtendedItemHandle) ( ( (List) input ).get( 0 ) );
		}
		else
		// input instanceof ExtendedItemHandle
		{
			handle = (ExtendedItemHandle) input;
		}

		try
		{
			crosstabHandle = (CrosstabReportItemHandle) handle.getReportItem( );
			return;
		}
		catch ( ExtendedElementException e )
		{
			// TODO Auto-generated catch block
			logger.log( Level.SEVERE, e.getMessage( ), e );
			return;
		}
	}

}
