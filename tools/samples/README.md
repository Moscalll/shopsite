# `samples/` 说明

以下 JSONL 描述的是 **要新上架的商品**（不是从网站自动同步的已上架数据）。`import_products.py` 只执行 **`INSERT`**；若需避免与已有商品 **同名同商户** 重复，请使用 `--skip-if-name-exists`。

| 文件                                       | 用途                                                                                   |
| ---------------------------------------- | ------------------------------------------------------------------------------------ |
| `product_spec.sample.jsonl`              | 最小示例（2 条），演示 `productDetailEn` / `showFrontLabel`                                    |
| `product_spec.shop467_8categories.jsonl` | **示例批次**：同一商户 `merchantId: 467`，覆盖分类 `1`～`8`，每条含 `**fullPromptEn`**（分类专属英文整段 prompt） |


生成命令示例：

```powershell
cd D:\GitHub\shopsite\tools
python product_pipeline\generate.py --in samples\product_spec.shop467_8categories.jsonl --out-dir out\shop467_run1
```

字段说明：

- `**merchantId**`：固定为你的商户用户 id（示例中为 `467`）。
- `**categoryId**`：每条对应一个分类；同一商户可在多分类下上架。
- `**fullPromptEn**`：整条英文用作 ComfyUI 正提示词；不需要再写 `productDetailEn`。
- 若不想自动追加防文字后缀，可在该行加 `"appendSdSafetySuffix": false`。

分类 **「收纳整理」(id=6)** 的长文案若你有更贴切的产品，可直接改 `shop467-c6` 那一行的 `fullPromptEn`。