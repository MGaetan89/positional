package io.trewartha.positional.ui.tracks

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.trewartha.positional.R
import io.trewartha.positional.storage.ViewModelFactory
import io.trewartha.positional.tracks.Track
import io.trewartha.positional.ui.track.TrackEditActivity
import kotlinx.android.synthetic.main.tracks_fragment.*

class TracksFragment : Fragment() {

    companion object {
        private const val REQUEST_CODE_EDIT_TRACK = 1
    }

    private val adapter = TracksAdapter({ _, track -> onTrackClick(track) })

    private lateinit var tracksViewModel: TracksViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        tracksViewModel = ViewModelProviders.of(
                this,
                ViewModelFactory()).get(TracksViewModel::class.java
        )
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return layoutInflater.inflate(R.layout.tracks_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tracksViewModel.tracks.observe(this, Observer {
            adapter.setList(it)
            if (it?.size ?: 0 <= 0) {
                showEmptyView()
            } else {
                hideEmptyView()
            }
        })
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_EDIT_TRACK -> when (resultCode) {
                TrackEditActivity.RESULT_DELETE_FAILED -> showDeleteResultSnackbar(false)
                TrackEditActivity.RESULT_DELETE_SUCCESSFUL -> showDeleteResultSnackbar(true)
                TrackEditActivity.RESULT_SAVE_FAILED -> showSaveResultSnackbar(false)
                TrackEditActivity.RESULT_SAVE_SUCCESSFUL -> showSaveResultSnackbar(true)
            }
        }
    }

    private fun hideEmptyView() {
        emptyView.clearAnimation()
        recyclerView.clearAnimation()

        val emptyViewAnimator = emptyView.animate()
                .alpha(0.0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        animation.removeListener(this)
                    }
                })

        val recyclerViewAnimator = recyclerView.animate()
                .alpha(1.0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        recyclerView.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        animation.removeListener(this)
                    }
                })

        emptyViewAnimator.start()
        recyclerViewAnimator.start()
    }

    private fun onTrackClick(track: Track) {
        val intent = TrackEditActivity.IntentBuilder(context)
                .withTrackId(track.id)
                .build()
        startActivityForResult(intent, REQUEST_CODE_EDIT_TRACK)
    }

    private fun showDeleteResultSnackbar(deleteSuccessful: Boolean) {
        val snackbarText = getString(
                if (deleteSuccessful)
                    R.string.track_delete_success_snackbar
                else
                    R.string.track_delete_failure_snackbar
        )
        Snackbar.make(coordinatorLayout, snackbarText, Snackbar.LENGTH_LONG).show()
    }

    private fun showSaveResultSnackbar(saveSuccessful: Boolean) {
        val snackbarText = getString(
                if (saveSuccessful)
                    R.string.track_save_success_snackbar
                else
                    R.string.track_save_failure_snackbar
        )
        Snackbar.make(coordinatorLayout, snackbarText, Snackbar.LENGTH_LONG).show()
    }

    private fun showEmptyView() {
        emptyView.clearAnimation()
        recyclerView.clearAnimation()

        val emptyViewAnimator = emptyView.animate()
                .alpha(1.0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        animation.removeListener(this)
                    }
                })
        val recyclerViewAnimator = recyclerView.animate()
                .alpha(0.0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        animation.removeListener(this)
                        recyclerView.visibility = View.GONE
                    }
                })

        emptyViewAnimator.start()
        recyclerViewAnimator.start()
    }
}