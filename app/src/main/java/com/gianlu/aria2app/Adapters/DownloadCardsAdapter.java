package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.gianlu.aria2app.DonutProgress;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Sorting.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DownloadCardsAdapter extends OrderedRecyclerViewAdapter<DownloadCardsAdapter.DownloadViewHolder, Download, DownloadCardsAdapter.SortBy, Download.Status> {
    private final Context context;
    private final IAdapter handler;
    private final LayoutInflater inflater;

    public DownloadCardsAdapter(Context context, List<Download> objs, IAdapter handler) {
        super(objs, SortBy.STATUS);
        this.context = context;
        this.handler = handler;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public DownloadCardsAdapter.DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DownloadViewHolder(parent);
    }

    private void setupActions(DownloadViewHolder holder, final Download download) {
        holder.start.setVisibility(View.VISIBLE);
        holder.stop.setVisibility(View.VISIBLE);
        holder.restart.setVisibility(View.VISIBLE);
        holder.pause.setVisibility(View.VISIBLE);
        holder.remove.setVisibility(View.VISIBLE);
        holder.moveUp.setVisibility(View.VISIBLE);
        holder.moveDown.setVisibility(View.VISIBLE);
        holder.more.setVisibility(View.VISIBLE);

        switch (download.status) {
            case ACTIVE:
                holder.restart.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.remove.setVisibility(View.GONE);
                holder.moveUp.setVisibility(View.GONE);
                holder.moveDown.setVisibility(View.GONE);
                break;
            case PAUSED:
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.GONE);
                holder.moveUp.setVisibility(View.GONE);
                holder.moveDown.setVisibility(View.GONE);
                break;
            case WAITING:
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.stop.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                break;
            case ERROR:
                holder.more.setVisibility(View.INVISIBLE);
            case COMPLETE:
            case REMOVED:
                if (download.isTorrent()) holder.restart.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.stop.setVisibility(View.GONE);
                holder.moveUp.setVisibility(View.GONE);
                holder.moveDown.setVisibility(View.GONE);
                break;
            case UNKNOWN:
                holder.more.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.stop.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.GONE);
                holder.moveUp.setVisibility(View.GONE);
                holder.moveDown.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onBindViewHolder(DownloadCardsAdapter.DownloadViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            holder.update((Download) payloads.get(0));
        }
    }

    @Override
    public void onBindViewHolder(final DownloadCardsAdapter.DownloadViewHolder holder, int position) {
        final Download item = objs.get(position);

        final int color;
        if (item.isTorrent())
            color = ContextCompat.getColor(context, R.color.colorTorrent_pressed);
        else color = ContextCompat.getColor(context, R.color.colorAccent);

        Utils.setupChart(holder.detailsChart, true);
        holder.detailsChart.setNoDataTextColor(color);
        holder.donutProgress.setFinishedStrokeColor(color);

        holder.detailsGid.setHtml(R.string.gid, item.gid);
        holder.detailsTotalLength.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(item.length, false));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CommonUtils.isExpanded(holder.details)) {
                    CommonUtils.collapse(holder.details);
                    CommonUtils.collapseTitle(holder.downloadName);
                } else {
                    CommonUtils.expand(holder.details);
                    CommonUtils.expandTitle(holder.downloadName);
                }
            }
        });

        holder.more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.onMoreClick(item);
            }
        });
        holder.pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onMenuItemSelected(item, JTA2.DownloadActions.PAUSE);
            }
        });
        holder.restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onMenuItemSelected(item, JTA2.DownloadActions.RESTART);
            }
        });
        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onMenuItemSelected(item, JTA2.DownloadActions.RESUME);
            }
        });
        holder.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onMenuItemSelected(item, JTA2.DownloadActions.REMOVE);
            }
        });
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onMenuItemSelected(item, JTA2.DownloadActions.REMOVE);
            }
        });
        holder.moveUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onMenuItemSelected(item, JTA2.DownloadActions.MOVE_UP);
            }
        });
        holder.moveDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null)
                    handler.onMenuItemSelected(item, JTA2.DownloadActions.MOVE_DOWN);
            }
        });

        setupActions(holder, item);

        holder.update(item);
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
        if (handler != null) handler.onItemCountUpdated(count);
    }

    @Override
    @NonNull
    public Comparator<Download> getComparatorFor(SortBy sorting) {
        switch (sorting) {
            default:
            case STATUS:
                return new Download.StatusComparator();
            case PROGRESS:
                return new Download.ProgressComparator();
            case DOWNLOAD_SPEED:
                return new Download.DownloadSpeedComparator();
            case UPLOAD_SPEED:
                return new Download.UploadSpeedComparator();
            case COMPLETED_LENGTH:
                return new Download.CompletedLengthComparator();
            case LENGTH:
                return new Download.LengthComparator();
        }
    }

    public enum SortBy {
        STATUS,
        PROGRESS,
        DOWNLOAD_SPEED,
        UPLOAD_SPEED,
        COMPLETED_LENGTH,
        LENGTH
    }

    public interface IAdapter {
        void onMoreClick(Download item);

        void onItemCountUpdated(int count);

        void onMenuItemSelected(Download download, JTA2.DownloadActions action);
    }

    class DownloadViewHolder extends RecyclerView.ViewHolder {
        final DonutProgress donutProgress;
        final SuperTextView downloadName;
        final SuperTextView downloadStatus;
        final SuperTextView downloadSpeed;
        final SuperTextView downloadMissingTime;
        final LinearLayout details;
        final SuperTextView detailsGid;
        final SuperTextView detailsTotalLength;
        final SuperTextView detailsCompletedLength;
        final SuperTextView detailsUploadLength;
        final ImageButton pause;
        final ImageButton start;
        final ImageButton stop;
        final ImageButton restart;
        final ImageButton remove;
        final ImageButton moveUp;
        final ImageButton moveDown;
        final Button more;
        final LineChart detailsChart;

        DownloadViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.download_card, parent, false));

            donutProgress = (DonutProgress) itemView.findViewById(R.id.downloadCard_donutProgress);
            downloadName = (SuperTextView) itemView.findViewById(R.id.downloadCard_name);
            downloadStatus = (SuperTextView) itemView.findViewById(R.id.downloadCard_status);
            downloadSpeed = (SuperTextView) itemView.findViewById(R.id.downloadCard_downloadSpeed);
            downloadMissingTime = (SuperTextView) itemView.findViewById(R.id.downloadCard_missingTime);
            details = (LinearLayout) itemView.findViewById(R.id.downloadCard_details);
            pause = (ImageButton) itemView.findViewById(R.id.downloadCard_pause);
            start = (ImageButton) itemView.findViewById(R.id.downloadCard_start);
            stop = (ImageButton) itemView.findViewById(R.id.downloadCard_stop);
            restart = (ImageButton) itemView.findViewById(R.id.downloadCard_restart);
            remove = (ImageButton) itemView.findViewById(R.id.downloadCard_remove);
            moveUp = (ImageButton) itemView.findViewById(R.id.downloadCard_moveUp);
            moveDown = (ImageButton) itemView.findViewById(R.id.downloadCard_moveDown);
            more = (Button) itemView.findViewById(R.id.downloadCard_actionMore);

            detailsChart = (LineChart) itemView.findViewById(R.id.downloadCard_detailsChart);
            detailsGid = (SuperTextView) itemView.findViewById(R.id.downloadCard_detailsGid);
            detailsTotalLength = (SuperTextView) itemView.findViewById(R.id.downloadCard_detailsTotalLength);
            detailsCompletedLength = (SuperTextView) itemView.findViewById(R.id.downloadCard_detailsCompletedLength);
            detailsUploadLength = (SuperTextView) itemView.findViewById(R.id.downloadCard_detailsUploadLength);
        }

        public void update(Download item) {
            if (item.status == Download.Status.ACTIVE) {
                LineData data = detailsChart.getData();
                if (data == null) {
                    Utils.setupChart(detailsChart, true);
                    data = detailsChart.getData();
                }

                if (data != null) {
                    int pos = data.getEntryCount() / 2 + 1;
                    data.addEntry(new Entry(pos, item.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                    data.addEntry(new Entry(pos, item.uploadSpeed), Utils.CHART_UPLOAD_SET);
                    data.notifyDataChanged();
                    detailsChart.notifyDataSetChanged();

                    detailsChart.setVisibleXRangeMaximum(90);
                    detailsChart.moveViewToX(pos - 91);
                }
            } else {
                detailsChart.clear();
                detailsChart.setNoDataText(context.getString(R.string.downloadIs, item.status.getFormal(context, false)));
            }

            donutProgress.setProgress(item.getProgress());
            downloadName.setText(item.getName());
            if (item.status == Download.Status.ERROR)
                downloadStatus.setText(String.format(Locale.getDefault(), "%s #%d: %s", item.status.getFormal(context, true), item.errorCode, item.errorMessage));
            else
                downloadStatus.setText(item.status.getFormal(context, true));
            downloadSpeed.setText(CommonUtils.speedFormatter(item.downloadSpeed, false));
            downloadMissingTime.setText(CommonUtils.timeFormatter(item.getMissingTime()));

            detailsCompletedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength, false));
            detailsUploadLength.setHtml(R.string.uploaded_length, CommonUtils.dimensionFormatter(item.uploadLength, false));

            setupActions(this, item);
        }
    }
}
