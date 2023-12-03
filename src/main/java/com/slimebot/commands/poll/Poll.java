package com.slimebot.commands.poll;

import com.slimebot.database.DataClass;
import com.slimebot.database.Key;
import com.slimebot.main.Main;
import com.slimebot.util.Util;
import io.leangen.geantyref.TypeToken;

import java.util.*;

public class Poll extends DataClass {
	@Key
	private final long id;

	private String json;

	private transient List<Long>[] values;

	private Poll() {
		id = 0;
	}

	public Poll(long id, int options) {
		this.id = id;
		this.values = Util.createListArray(options);
		generateJson();
	}


	public static Optional<Poll> getPoll(long id) {
		return load(Poll::new, Map.of("id", id));
	}

	@Override
	public synchronized Poll save() {
		generateJson();
		super.save();
		return this;
	}

	@Override
	protected void finishedLoading() {
		this.values = Main.gson.fromJson(json, new TypeToken<List<Long>[]>() {
		}.getType());
	}

	private void generateJson() {
		this.json = Main.gson.toJson(values);
	}

	private boolean remove(long member) {
		for (List<Long> value : values)
			if (value.remove(member)) return true;

		return false;
	}

	public enum Type {
		REMOVED,
		SET,
		REMOVED_SET
	}

	public Type set(int option, long member) {
		if (values[option].remove(member)) return Type.REMOVED;
		else {
			boolean flag = remove(member);
			values[option].add(member);

			return flag ? Type.REMOVED_SET : Type.SET;
		}
	}

	public List<Long>[] getOptions() {
		return values;
	}

	public List<Long> getAll() {
		return Arrays.stream(values)
				.flatMap(Collection::stream)
				.toList();
	}
}
