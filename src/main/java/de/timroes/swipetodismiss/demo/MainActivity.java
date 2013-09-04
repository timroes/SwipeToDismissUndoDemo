package de.timroes.swipetodismiss.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import de.timroes.swipetodismiss.SwipeDismissList;
import java.util.ArrayList;
import java.util.Arrays;
import  android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * A simple ListActivity (extending {@link android.support.v7.app.ActionBarActivity} for backwards
 * compatibility of the ActionBar.
 *
 * @author Tim Roes <mail@timroes.de>
 */
public class MainActivity extends  ActionBarActivity implements android.widget.AdapterView.OnItemClickListener{

	/**
	 * The adapter used to store the the list data. In this case an
	 * {@link ArrayAdapter} is used. This will be replaced by your adapter.
	 */
	private ArrayAdapter<String> mAdapter;
	/**
	 * The {@link SwipeDismissList} you generate during your
	 * {@link #onCreate(android.os.Bundle)} method.
	 */
	private SwipeDismissList mSwipeList;
	/**
	 * The key of the {@link Bundle} extra we store the mode of the list the
	 * user selected.
	 */
	private final static String EXTRA_MODE = "MODE";
    private ListView listView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        listView = (ListView) findViewById(R.id.currentLabelList);
        listView.setOnItemClickListener(this);

        // Extract the list mode from the bundle or 0 if none choosen yet.
        int modeInt = getIntent().getIntExtra("MODE", 0);
        SwipeDismissList.UndoMode mode = SwipeDismissList.UndoMode.values()[modeInt];

        // Sets the subtitle in the ActionBar to the current mode.
        getSupportActionBar().setSubtitle(String.format("Mode: %s", mode.toString()));

        // Get the regular ListView of this activity.
        ListView listView = getListView();

        // Create a new SwipeDismissList from the activities listview.
        mSwipeList = new SwipeDismissList(
                // 1st parameter is the ListView you want to use
                listView,
                // 2nd parameter is an OnDismissCallback, that handles the deletion
                // and undo of list items. It only needs to implement onDismiss.
                // This method can return an Undoable (then this deletion can be undone)
                // or null (if the user shouldn't get the possibility to undo the
                // deletion).
                new SwipeDismissList.OnDismissCallback() {
                    /**
                     * Will be called, whenever the user swiped out an list item.
                     *
                     * @param listView The {@link ListView} that the item was deleted
                     * from.
                     * @param position The position of the item, that was deleted.
                     * @return An {@link Undoable} or {@code null} if this deletion
                     * shouldn't be undoable.
                     */
                    public SwipeDismissList.Undoable onDismiss(AbsListView listView, final int position) {

                        // Get item that should be deleted from the adapter.
                        final String item = mAdapter.getItem(position);
                        // Delete that item from the adapter.
                        mAdapter.remove(item);

                        // Return an Undoable, for that deletion. If you write return null
                        // instead, this deletion won't be undoable.
                        return new SwipeDismissList.Undoable() {
                            /**
                             * Optional method. If you implement this method, the
                             * returned String will be presented in the undo view to the
                             * user.
                             */
                            @Override
                            public String getTitle() {
                                return item + " deleted";
                            }

                            /**
                             * Will be called when the user hits undo. You want to
                             * reinsert the item to the adapter again. The library will
                             * always call undo in the reverse order the item has been
                             * deleted. So you can insert the item at the position it
                             * was deleted from, unless you have modified the list
                             * (added or removed items) somewhere else in your activity.
                             * If you do so, you might want to call
                             * {@link SwipeDismissList#discardUndo()}, so the user
                             * cannot undo the action anymore. If you still want the
                             * user to be able to undo the deletion (after you modified
                             * the list somewhere else) you will need to calculate the
                             * new position of this item yourself.
                             */
                            @Override
                            public void undo() {
                                // Reinsert the item at its previous position.
                                mAdapter.insert(item, position);
                            }

                            /**
                             * Will be called, when the user doesn't have the
                             * possibility to undo the action anymore. This can either
                             * happen, because the undo timed out or
                             * {@link SwipeDismissList#discardUndo()} was called. If you
                             * have stored your objects somewhere persistent (e.g. a
                             * database) you might want to use this method to delete the
                             * object from this persistent storage.
                             */
                            @Override
                            public void discard() {
                                // Just write a log message (use logcat to see the effect)
                                Log.w("DISCARD", "item " + item + " now finally discarded");
                            }
                        };

                    }
                },
                // 3rd parameter needs to be the mode the list is generated.
                mode);

        // If we have a MULTI_UNDO list (several items can be undone one by one),
        // set the UndoMultipleString to null. If you set this to null the undo popup
        // will show the title of the item that will be undone next. If you don't
        // set this to null (leave it default, or set some other string), the string
        // will be shown (and first placeholder %d replaced with number of pending undos).
        if (mode == SwipeDismissList.UndoMode.MULTI_UNDO) {
            mSwipeList.setUndoMultipleString(null);
        }

        // Just reset the adapter.
        resetAdapter();

    }

	/**
	 * When the activity is stopped, discard all undos. If you don't do this,
	 * {@link SwipeDismissList.Undoable#discard()} is not guaranteed to be
	 * called for every item. If you use the discard method to delete items from
	 * database, not calling discardUndo() will result in bugs.
	 */
	@Override
	protected void onStop() {
		super.onStop();
		// Throw away all pending undos.
		mSwipeList.discardUndo();
	}

	/**
	 * Create the option menu from xml file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Handle click on a menu item.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Show information dialog.
			case R.id.about:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("This is a demonstration of the SwipeToDismissUndo library "
					+ "of Tim Roes. The lib helps you getting the swipe to dismiss on list views "
					+ "and enable undo functionality. Visit the project's page for more information.");
				builder.setTitle("SwipeToDismissUndo Library Demo");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.setNeutralButton("Visit Page", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent in = new Intent(Intent.ACTION_VIEW,
							Uri.parse("https://github.com/timroes/SwipeToDismissUndoList"));
						in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(in);
						dialog.dismiss();
					}
				});
				builder.create().show();
				break;
			// Reset the list to its initial state.
			case R.id.reset:
				resetAdapter();
				break;
			// Let the user select a mode and restart activity with that mode.
			// Not the best behavior to restart the app.. but this i just an API demo!
			case R.id.switch_mode:
				AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setTitle("Pick Undo Mode")
					.setItems(new String[]{
						SwipeDismissList.UndoMode.values()[0].name(),
						SwipeDismissList.UndoMode.values()[1].name(),
						SwipeDismissList.UndoMode.values()[2].name()
					}, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent in = new Intent(MainActivity.this, MainActivity.class);
						in.putExtra(EXTRA_MODE, which);
						mSwipeList.discardUndo();
						finish();
						startActivity(in);
					}
				}).create().show();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Loads some simple strings into the list adapter.
	 */
	private void resetAdapter() {

		mSwipeList.discardUndo();

		String[] items = new String[20];
		for (int i = 0; i < items.length; i++) {
			items[i] = String.format("Test Item %d", i);
		}

		mAdapter = new ArrayAdapter<String>(this,
			android.R.layout.simple_list_item_1,
			android.R.id.text1,
			new ArrayList<String>(Arrays.asList(items)));
		getListView().setAdapter(mAdapter);

	}

	/**
	 * Show a toast, when a list item is clicked.
	 */
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Toast.makeText(this, String.format("Clicked on item #%d with text %s",
			position, mAdapter.getItem(position)), Toast.LENGTH_SHORT).show();
	}

    public ListView getListView() {
        return listView;
    }
}
