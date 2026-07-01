# 智慧烟感系统 - K8s 部署指南

## 架构说明

```
┌─────────────────────────────────────────────────────┐
│           Kubernetes Cluster (3台电脑)              │
│                                                     │
│  电脑A (Master)  │  电脑B (Worker)  │  电脑C (Worker)│
│  ┌────────────┐  │  ┌────────────┐  │  ┌────────┐  │
│  │  K8s API   │  │  │  Backend   │  │  │ MySQL  │  │
│  │  Scheduler │  │  │  Pod x2    │  │  │  Pod   │  │
│  │  etcd      │  │  │            │  │  │        │  │
│  └────────────┘  │  └────────────┘  │  └────────┘  │
│                  │                  │               │
│  ┌────────────┐  │                  │               │
│  │  Frontend  │  │                  │               │
│  │  Pod x2    │  │                  │               │
│  └────────────┘  │                  │               │
└─────────────────────────────────────────────────────┘
         ↑
    http://<任意节点IP>:30080
```

## 前置要求

1. 三台电脑装好 Docker 和 K8s（kubeadm）
2. 电脑A作为Master，电脑B和C作为Worker
3. 网络互通（能互相ping通）

## 快速部署

```bash
# 1. 构建镜像（在有代码的电脑上执行）

# 前端镜像
cd frontend/
docker build -t your-dockerhub/smoke-frontend:latest .
docker push your-dockerhub/smoke-frontend:latest

# 后端镜像
cd backend/
docker build -t your-dockerhub/smoke-backend:latest .
docker push your-dockerhub/smoke-backend:latest

# 2. 部署到K8s
chmod +x deploy.sh
./deploy.sh

# 3. 访问
# 浏览器打开 http://<任意节点IP>:30080
```

## 手动部署（不用脚本）

```bash
# 创建资源
kubectl apply -f k8s-deployment.yaml

# 查看Pod状态
kubectl get pods -n smoke-sensor

# 查看服务
kubectl get svc -n smoke-sensor

# 查看日志
kubectl logs -f deployment/backend -n smoke-sensor

# 删除所有资源
kubectl delete namespace smoke-sensor
```

## 网络说明

- **MySQL**：ClusterIP，只在集群内部访问
- **Backend**：ClusterIP，前端通过nginx代理访问
- **Frontend**：NodePort 30080，外部通过 `http://<节点IP>:30080` 访问

## 常用命令

```bash
# 进入Pod调试
kubectl exec -it <pod-name> -n smoke-sensor -- /bin/sh

# 端口转发（本地调试用）
kubectl port-forward svc/frontend 8080:80 -n smoke-sensor
# 然后访问 http://localhost:8080

# 扩容后端
kubectl scale deployment/backend --replicas=3 -n smoke-sensor

# 滚动更新
kubectl set image deployment/backend backend=your-dockerhub/smoke-backend:v2 -n smoke-sensor

# 回滚
kubectl rollout undo deployment/backend -n smoke-sensor
```

## 故障排查

```bash
# Pod启动失败
kubectl describe pod <pod-name> -n smoke-sensor

# 查看Pod日志
kubectl logs <pod-name> -n smoke-sensor

# 查看事件
kubectl get events -n smoke-sensor --sort-by='.lastTimestamp'
```

## 生产环境优化

1. **持久化存储**：MySQL用 PersistentVolume
2. **Secret管理**：密码用K8s Secret，不用明文ConfigMap
3. **资源限制**：给Pod加CPU/内存限制
4. **健康检查**：配置readinessProbe和livenessProbe
5. **HPA自动扩缩容**：根据CPU使用率自动扩Pod
6. **监控**：部署Prometheus + Grafana

## 参考

- [K8s官方文档](https://kubernetes.io/zh/docs/)
- [Kubeadm安装指南](https://kubernetes.io/zh/docs/setup/production-environment/tools/kubeadm/)
