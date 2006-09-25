/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.designer.ui.editors;

import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.designer.internal.ui.ide.adapters.IDEFileReportProvider;
import org.eclipse.birt.report.designer.internal.ui.ide.adapters.LibraryProvider;
import org.eclipse.birt.report.designer.internal.ui.views.ILibraryProvider;
import org.eclipse.birt.report.designer.nls.Messages;
import org.eclipse.birt.report.model.api.ErrorDetail;
import org.eclipse.birt.report.model.api.LibraryHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Use this class to activate IDE plug-in.
 */

public class IDEMultiPageReportEditor extends MultiPageReportEditor
{

	/**
	 * Report element ID marker attribute. It's used to record the report
	 * element in the marker.
	 * 
	 * @see #getAttribute(String, String)
	 */
	public static final String ELEMENT_ID = "ElementId"; //$NON-NLS-1$ 

	private static final String DLG_SAVE_BUTTON_CLOSE = Messages.getString( "ReportEditor.Button.Close" ); //$NON-NLS-1$

	private static final String DLG_SAVE_BUTTON_SAVE = Messages.getString( "ReportEditor.Button.Save" ); //$NON-NLS-1$

	private static final String DLG_SAVE_CONFIRM_DELETE = Messages.getString( "ReportEditor.Dlg.Confirm" ); //$NON-NLS-1$

	private static final String DLG_SAVE_TITLE = Messages.getString( "ReportEditor.Dlg.Save" ); //$NON-NLS-1$

	private boolean isWorkspaceResource = false;

	private ResourceTracker resourceListener = new ResourceTracker( );

	class ResourceTracker implements
			IResourceChangeListener,
			IResourceDeltaVisitor
	{

		/**
		 * does when resource changed
		 */
		public void resourceChanged( IResourceChangeEvent event )
		{
			IResourceDelta delta = event.getDelta( );
			try
			{
				if ( delta != null )
					delta.accept( this );
			}
			catch ( CoreException exception )
			{
				// What should be done here?
			}
		}

		/**
		 * is visit successful
		 */
		public boolean visit( IResourceDelta delta )
		{
			if ( delta == null
					|| !delta.getResource( )
							.equals( getFile( getEditorInput( ) ) ) )
				return true;

			if ( delta.getKind( ) == IResourceDelta.REMOVED )
			{
				Display display = getSite( ).getShell( ).getDisplay( );
				if ( ( IResourceDelta.MOVED_TO & delta.getFlags( ) ) == 0 )
				{
					// if the file was deleted.
					// NOTE: The case where an open, unsaved file is deleted is
					// being handled by the PartListener added to the Workbench
					// in the initialize() method.
					display.asyncExec( new Runnable( ) {

						public void run( )
						{
							if ( !isDirty( ) )
							{
								closeEditor( false );
							}
							else
							{
								String title = DLG_SAVE_TITLE;
								String message = DLG_SAVE_CONFIRM_DELETE;
								String[] buttons = {
										DLG_SAVE_BUTTON_SAVE,
										DLG_SAVE_BUTTON_CLOSE
								};
								MessageDialog dialog = new MessageDialog( getSite( ).getShell( ),
										title,
										null,
										message,
										MessageDialog.QUESTION,
										buttons,
										0 );
								if ( dialog.open( ) == Dialog.OK )
								{
									doSaveAs( );
								}
								else
								{
									closeEditor( false );
								}
							}
						}
					} );
				}
				else
				{ // else if it was moved or renamed
					final IFile newFile = ResourcesPlugin.getWorkspace( )
							.getRoot( )
							.getFile( delta.getMovedToPath( ) );
					display.asyncExec( new Runnable( ) {

						public void run( )
						{
							FileEditorInput input = new FileEditorInput( newFile );
							setAllInput( input );
						}
					} );
				}
			}
			else if ( delta.getKind( ) == IResourceDelta.CHANGED )
			{
				// final IFile newFile = ResourcesPlugin.getWorkspace( )
				// .getRoot( )
				// .getFile( delta.getFullPath( ) );
				// Display display = getSite( ).getShell( ).getDisplay( );
				// if ( ( delta.getFlags( ) & IResourceDelta.MARKERS ) == 0 )
				// {
				// // The file was overwritten somehow (could have been
				// // replaced by another
				// // version in the repository)
				// display.asyncExec( new Runnable( ) {
				//
				// public void run( )
				// {
				// }
				// } );
				// }
				// else if ( isEditorSaving( ) )
				// {
				// display.asyncExec( new Runnable( ) {
				//
				// public void run( )
				// {
				// try
				// {
				// refreshMarkers( getEditorInput( ) );
				// }
				// catch ( CoreException e )
				// {
				// ExceptionHandler.handle( e );
				// }
				// }
				// } );
				// }
			}
			return false;
		}

		private void setAllInput( FileEditorInput input )
		{
			// This for a bug. When close editor, the resource listener fire to
			// multi page editor, but all embedded pages disposed.
			if ( pages == null )
			{
				return;
			}

			setInput( input );

			if ( getEditorInput( ) != null )
			{
				setPartName( getEditorInput( ).getName( ) );
				firePropertyChange( IWorkbenchPartConstants.PROP_PART_NAME );
				firePropertyChange( IWorkbenchPartConstants.PROP_PART_NAME );
				getProvider( ).getReportModuleHandle( getEditorInput( ) )
						.setFileName( getProvider( ).getInputPath( getEditorInput( ) )
								.toOSString( ) );
			}

			for ( Iterator it = pages.iterator( ); it.hasNext( ); )
			{
				Object page = it.next( );
				if ( page instanceof IReportEditorPage )
				{
					( (IReportEditorPage) page ).setInput( input );
				}
			}
		}
	}

	protected void setInput( IEditorInput input )
	{

		// The workspace never changes for an editor. So, removing and re-adding
		// the
		// resourceListener is not necessary. But it is being done here for the
		// sake
		// of proper implementation. Plus, the resourceListener needs to be
		// added
		// to the workspace the first time around.
		if ( isWorkspaceResource )
		{
			getFile( getEditorInput( ) ).getWorkspace( )
					.removeResourceChangeListener( resourceListener );
		}

		isWorkspaceResource = input instanceof IFileEditorInput;

		super.setInput( input );

		if ( isWorkspaceResource )
		{
			getFile( getEditorInput( ) ).getWorkspace( )
					.addResourceChangeListener( resourceListener );
		}
	}

	private IFile getFile( IEditorInput editorInput )
	{
		if ( isWorkspaceResource )
		{
			return ( (IFileEditorInput) editorInput ).getFile( );
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor#partActivated(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partActivated( IWorkbenchPart part )
	{
		super.partActivated( part );
		if ( part != this )

			return;
		if ( isWorkspaceResource )
		{
			if ( !( (IFileEditorInput) getEditorInput( ) ).getFile( ).exists( ) )
			{

				Shell shell = getSite( ).getShell( );

				String title = DLG_SAVE_TITLE;

				String message = DLG_SAVE_CONFIRM_DELETE;

				String[] buttons = {
						DLG_SAVE_BUTTON_SAVE, DLG_SAVE_BUTTON_CLOSE
				};

				MessageDialog dialog = new MessageDialog( shell,
						title,
						null,
						message,
						MessageDialog.QUESTION,
						buttons,
						0 );

				if ( dialog.open( ) == 0 )
				{
					doSaveAs( );
					partActivated( part );
				}

				else
				{
					closeEditor( false );
				}

			}
		}
	}

	protected void addPages( )
	{
		super.addPages( );
		if ( isWorkspaceResource )
		{
			try
			{
				refreshMarkers( getEditorInput( ) );
			}
			catch ( CoreException e )
			{
				e.printStackTrace( );
			}
		}
	}

	/**
	 * Deletes existed problem markers and adds new markers
	 * 
	 * @throws CoreException
	 */
	protected void refreshMarkers( IEditorInput input ) throws CoreException
	{
		IResource file = getFile( input );

		// Deletes existed markers
		file.deleteMarkers( IMarker.PROBLEM, true, IResource.DEPTH_INFINITE );

		// Adds markers
		ModuleHandle reportDesignHandle = getModel( );
		if ( reportDesignHandle == null )
		{
			return;
		}
		
		// Model said that should checkReport( ) before getting error and warning list.
		reportDesignHandle.checkReport( );
		List list = reportDesignHandle.getErrorList( );
		int errorListSize = list.size( );
		list.addAll( reportDesignHandle.getWarningList( ) );

		for ( int i = 0, m = list.size( ); i < m; i++ )
		{
			ErrorDetail errorDetail = (ErrorDetail) list.get( i );
			IMarker marker = file.createMarker( IMarker.PROBLEM );

			// The first part is from error list, the other is from warning list
			if ( i < errorListSize )
				marker.setAttribute( IMarker.SEVERITY, IMarker.SEVERITY_ERROR );
			else
				marker.setAttribute( IMarker.SEVERITY, IMarker.SEVERITY_WARNING );
			marker.setAttribute( IMarker.MESSAGE, errorDetail.getMessage( ) );
			marker.setAttribute( IMarker.LINE_NUMBER, errorDetail.getLineNo( ) );
			marker.setAttribute( IMarker.LOCATION, errorDetail.getTagName( ) );

			if ( errorDetail.getElement( ) != null
					&& errorDetail.getElement( ).getID( ) != 0 )
			{
				marker.setAttribute( ELEMENT_ID,
						new Integer( (int) errorDetail.getElement( ).getID( ) ) );
			}


		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave( IProgressMonitor monitor )
	{
		super.doSave( monitor );
		try
		{
			refreshMarkers( getEditorInput( ) );
		}
		catch ( CoreException e )
		{
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor#dispose()
	 */
	public void dispose( )
	{
		try
		{
			clearMarkers( );
		}
		catch ( CoreException e )
		{
		}
		if ( isWorkspaceResource )
		{
			getFile( getEditorInput( ) ).getWorkspace( )
					.removeResourceChangeListener( resourceListener );
		}
		super.dispose( );
		reportProvider = null;
	}

	/**
	 * Deletes all markers
	 * 
	 * @throws CoreException
	 */
	protected void clearMarkers( ) throws CoreException
	{
		IResource resource = getFile( getEditorInput( ) );
		if ( resource.exists( ) )
		{
			resource.deleteMarkers( IMarker.PROBLEM, true, IResource.DEPTH_ONE );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.editors.MultiPageReportEditor#getAdapter(java.lang.Class)
	 */
	public Object getAdapter( Class type )
	{
		if ( type == ILibraryProvider.class )
		{
			return new LibraryProvider( );
		}

		if ( type == IReportProvider.class )
		{

			return getProvider( );
		}

		if ( type == IGotoMarker.class )
		{
			return new BIRTGotoMarker( this );
		}

		return super.getAdapter( type );
	}

	protected IReportProvider getProvider( )
	{
		if ( reportProvider == null )
		{
			reportProvider = new IDEFileReportProvider( );
		}
		return reportProvider;
	}

}
