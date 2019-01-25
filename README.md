# Demo for monitoring DMN with Prometheus

### Example queries

rate(dmn_evaluate_decision_nanosecond_bucket[5m])
dmn_evaluate_decision_nanosecond_sum / dmn_evaluate_decision_nanosecond_count
histogram_quantile(0.5, rate(dmn_evaluate_decision_nanosecond_bucket[5m]))
avg_over_time(dmn_evaluate_decision_nanosecond_bucket[5m])
max_over_time(dmn_evaluate_decision_nanosecond_bucket[5m])

histogram_quantile(
    0.5,
    sum without (instance)(rate(dmn_evaluate_decision_nanosecond_bucket[5s])))
    
        

