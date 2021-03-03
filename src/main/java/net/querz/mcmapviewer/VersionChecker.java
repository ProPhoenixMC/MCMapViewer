package net.querz.mcmapviewer;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;

public class VersionChecker {

	private static final String endpointTemplate = "https://api.github.com/repos/%s/%s/releases/latest";

	private final String owner;
	private final String repository;

	public VersionChecker(String owner, String repository) {
		this.owner = owner;
		this.repository = repository;
	}

	public VersionData fetchLatestVersion() throws Exception {
		String endpoint = String.format(endpointTemplate, owner, repository);
		URL url = new URL(endpoint);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}
		}
		return parseJson(stringBuilder.toString());
	}

	private VersionData parseJson(String json) throws JSONException {
		JSONObject result = new JSONObject(json);

		int latestID = 0;
		String latestTag = null;
		String latestLink = null;
		boolean prerelease = false;

		for (String key : result.keySet()) {
			if ("id".equals(key)) {
				latestID = result.getInt("id");
			} else if ("tag_name".equals(key)) {
				latestTag = result.getString("tag_name");
			} else if ("html_url".equals(key)) {
				latestLink = result.getString("html_url");
			} else if ("prerelease".equals(key)) {
				prerelease = result.getBoolean("prerelease");
			}
		}

		if (latestID == 0) {
			return null;
		}

		return new VersionData(latestID, latestTag, latestLink, prerelease);
	}

	public static class VersionData {
		private final int id;
		private final String tag, link;
		private final boolean prerelease;

		private VersionData(int id, String tag, String link, boolean prerelease) {
			this.id = id;
			this.tag = tag;
			this.link = link;
			this.prerelease = prerelease;
		}

		public boolean isNewerThan(VersionData version) {
			return id > version.id;
		}

		public boolean isOlderThan(VersionData version) {
			return id < version.id;
		}

		public boolean isNewerThan(String tag) {
			return compare(this.tag, tag) > 0;
		}

		private int compare(String a, String b) {
			String[] split = a.split("\\.");
			String[] splitOther = b.split("\\.");
			int length = Math.max(split.length, splitOther.length);

			for (int i = 0; i < length; i++) {
				String me = i < split.length ? split[i] : "0";
				String you = i < splitOther.length ? splitOther[i] : "0";
				int meInt = withDefault(() -> Integer.parseInt(me), 0);
				int youInt = withDefault(() -> Integer.parseInt(you), 0);

				int comp = Integer.compare(meInt, youInt);
				if (comp != 0) {
					return comp;
				}
			}
			return 0;
		}

		private <T> T withDefault(Supplier<T> supplier, T def) {
			try {
				return supplier.get();
			} catch (Exception ex) {
				return def;
			}
		}

		public boolean isOlderThan(String tag) {
			return compare(this.tag, tag) < 0;
		}

		public String getTag() {
			return tag;
		}

		public String getLink() {
			return link;
		}

		public boolean isPrerelease() {
			return prerelease;
		}

		@Override
		public String toString() {
			return "id=" + id + ", tag=" + tag + ", link=" + link + ", prerelease=" + prerelease;
		}
	}
}
