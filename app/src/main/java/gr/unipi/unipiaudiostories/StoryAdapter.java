package gr.unipi.unipiaudiostories;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryVH> {

    public interface OnStoryClick {
        void onClick(Story story);
    }

    private final Context context;
    private final List<Story> stories;
    private final OnStoryClick listener;

    public StoryAdapter(Context context, List<Story> stories, OnStoryClick listener) {
        this.context = context;
        this.stories = stories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new StoryVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryVH h, int pos) {
        Story s = stories.get(pos);
        h.tvTitle.setText(s.title);
        h.tvAuthor.setText(s.author);

        Glide.with(context)
                .load(s.imageUrl)
                .centerCrop()
                .into(h.img);

        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    static class StoryVH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvTitle, tvAuthor;

        StoryVH(View v) {
            super(v);
            img = v.findViewById(R.id.imgStory);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvAuthor = v.findViewById(R.id.tvAuthor);
        }
    }
}
