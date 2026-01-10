import Handlebars from "handlebars";
import { readFile, writeFile } from "node:fs/promises";

Handlebars.registerHelper("extractGithubPath", function (repoUrl) {
  if (!repoUrl) return "";
  const match = repoUrl.match(/github\.com\/([^\/]+\/[^\/]+)/);
  return match ? match[1].replace(/\.git$/, "") : "";
});

Handlebars.registerHelper("hasAppId", function (appId, options) {
  return appId ? options.fn(this) : options.inverse(this);
});

async function gen() {
  try {
    const data = JSON.parse(await readFile("data.json", "utf-8"));
    const plugins = data.filter((item) => item.type === "plugin");
    const officialPlugins = plugins.filter((item) => item.official);
    const communityPlugins = plugins.filter((item) => !item.official);
    const themes = data.filter((item) => item.type === "theme");
    const officialThemes = themes.filter((item) => item.official);
    const communityThemes = themes.filter((item) => !item.official);
    const other = data.filter((item) => item.type === "other");

    const templateContent = await readFile("README.hbs", "utf-8");
    const template = Handlebars.compile(templateContent);

    const result = template({
      officialPlugins,
      communityPlugins,
      officialThemes,
      communityThemes,
      other,
    });

    await writeFile("README.md", result, "utf-8");
    console.log("README.md generated successfully!");
  } catch (error) {
    console.error("Error generating README:", error.message);
    process.exit(1);
  }
}

await gen();
