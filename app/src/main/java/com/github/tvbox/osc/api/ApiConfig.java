                        }
                    }

                    @Override
                    public void onError(Response<File> response) {
                        Throwable ex = response.getException();
                        if (ex != null) {
                            LOG.i("echo---jar Request failed: " + ex.getMessage());
                        }
                        if(cache.exists())jarLoader.load(cache.getAbsolutePath());
                        callback.error(ex != null ? "从网络上加载jar失败：" + ex.getMessage() : "未知网络错误");
                    }
                });
    }

    private void parseJson(String apiUrl, File f) throws Throwable {
        System.out.println("从本地缓存加载" + f.getAbsolutePath());
        BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String s = "";
        while ((s = bReader.readLine()) != null) {
            sb.append(s + "\n");
        }
        bReader.close();
        parseJson(apiUrl, sb.toString());
    }

    private static  String jarCache ="true";
    private void parseJson(String apiUrl, String jsonStr) {
//        pyLoader.setConfig(jsonStr);
        JsonObject infoJson = new Gson().fromJson(jsonStr, JsonObject.class);
        jarCache = DefaultConfig.safeJsonString(infoJson, "jarCache", "true");
        // spider
        spider = DefaultConfig.safeJsonString(infoJson, "spider", "");
        // wallpaper
        wallpaper = DefaultConfig.safeJsonString(infoJson, "wallpaper", "");
        // 直播播放请求头
        livePlayHeaders = infoJson.getAsJsonArray("livePlayHeaders");
        // 远端站点源
        SourceBean firstSite = null;
        JsonArray sites = infoJson.has("video") ? infoJson.getAsJsonObject("video").getAsJsonArray("sites") : infoJson.get("sites").getAsJsonArray();
        for (JsonElement opt : sites) {
            JsonObject obj = (JsonObject) opt;
            SourceBean sb = new SourceBean();
            String siteKey = obj.get("key").getAsString().trim();
            sb.setKey(siteKey);
            sb.setName(obj.has("name")?obj.get("name").getAsString().trim():siteKey);
            sb.setType(obj.get("type").getAsInt());
            sb.setApi(obj.get("api").getAsString().trim());
            sb.setSearchable(DefaultConfig.safeJsonInt(obj, "searchable", 1));
            sb.setQuickSearch(DefaultConfig.safeJsonInt(obj, "quickSearch", 1));
            if(siteKey.startsWith("py_")){
                sb.setFilterable(1);
            }else {
                sb.setFilterable(DefaultConfig.safeJsonInt(obj, "filterable", 1));
            }
            sb.setHide(DefaultConfig.safeJsonInt(obj, "hide", 0));
            sb.setPlayerUrl(DefaultConfig.safeJsonString(obj, "playUrl", ""));
            sb.setExt(DefaultConfig.safeJsonString(obj, "ext", ""));
            sb.setJar(DefaultConfig.safeJsonString(obj, "jar", ""));
            sb.setPlayerType(DefaultConfig.safeJsonInt(obj, "playerType", -1));
            sb.setCategories(DefaultConfig.safeJsonStringList(obj, "categories"));
            sb.setClickSelector(DefaultConfig.safeJsonString(obj, "click", ""));
            sb.setStyle(DefaultConfig.safeJsonString(obj, "style", ""));
            if (firstSite == null && sb.getHide() == 0)
                firstSite = sb;
            sourceBeanList.put(siteKey, sb);
        }
        if (sourceBeanList != null && sourceBeanList.size() > 0) {
            String home = Hawk.get(HawkConfig.HOME_API, "");
            SourceBean sh = getSource(home);
            if (sh == null || sh.getHide() == 1)
                setSourceBean(firstSite);
            else
                setSourceBean(sh);
        }
        // 需要使用vip解析的flag
        vipParseFlags = DefaultConfig.safeJsonStringList(infoJson, "flags");
        // ===================== 解析处理 =====================
parseBeanList.clear();

// 1. 加载配置中的解析
if (infoJson.has("parses")) {
    JsonArray parses = infoJson.get("parses").getAsJsonArray();
    for (JsonElement opt : parses) {
        JsonObject obj = (JsonObject) opt;
        ParseBean pb = new ParseBean();
        pb.setName(obj.get("name").getAsString().trim());
        pb.setUrl(obj.get("url").getAsString().trim());
        String ext = obj.has("ext") ? obj.get("ext").getAsJsonObject().toString() : "";
        pb.setExt(ext);
        pb.setType(DefaultConfig.safeJsonInt(obj, "type", 0));
        parseBeanList.add(pb);
    }
    
    if(!parseBeanList.isEmpty()){
        addSuperParse();
    }
}

// 2. 添加默认解析（修改名称）
if (parseBeanList.isEmpty()) {
    ParseBean defaultPb = new ParseBean();
    defaultPb.setName("");  // 改为空字符串
    defaultPb.setUrl("https://jx.84jia.com/api/?key=JBPsZJyg2q5Vn3nZP3&url=");
    defaultPb.setExt("");
    defaultPb.setType(1);
    parseBeanList.add(defaultPb);
}

// 3. 设置默认解析
if (!parseBeanList.isEmpty()) {
    String defaultParse = Hawk.get(HawkConfig.DEFAULT_PARSE, "");
    if (!TextUtils.isEmpty(defaultParse)) {
        for (ParseBean pb : parseBeanList) {
            if (pb.getName().equals(defaultParse)) {
                setDefaultParse(pb);
                break;
            }
        }
    }
    if (mDefaultParse == null) {
