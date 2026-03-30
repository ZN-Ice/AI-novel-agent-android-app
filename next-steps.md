# 下一步操作指南

## 已完成
✅ 修复 JaCoCo executionData 配置（从具体路径改为通配符 **/*.exec）
✅ 提交并推送到 feat/continue-tasks-v2 分支

## 下一步：在本地运行测试

打开终端，进入项目目录并运行以下命令：

```bash
cd C:\Users\zhang\2026\study\AI-novel-agent-android-app
./gradlew testDebugUnitTest createDebugUnitTestCoverageReport
```

## 检查结果

1. 测试应该全部通过
2. 覆盖率报告位置：`app/build/reports/coverage/test/debug/index.html`
3. 打开覆盖率报告，检查覆盖率是否不再是 0%

## 如果覆盖率正常

运行以下命令创建 PR：

```bash
gh pr create --base main --head feat/continue-tasks-v2 --title "fix: 修复测试覆盖率配置" --body "## 变更内容

- 修复 JaCoCo executionData 路径配置
- 从具体路径改为通配符 **/*.exec

## 测试计划
- [x] 本地构建验证
- [x] 覆盖率报告生成
- [ ] CI 门禁检查"
```

## 如果覆盖率仍然是 0%

请告诉我，我会继续排查问题。
