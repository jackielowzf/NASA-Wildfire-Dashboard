
function HistoryCalendar({
  history,
  selectedDate,
  setSelectedDate
}) {

  const maxCount = Math.max(
    1,
    ...history.map(day => day.totalCount)
  );

  function getActivityColor(count) {
    const intensity = count / maxCount;

    if (intensity > 0.8) return "#b82a21";
    if (intensity > 0.6) return "#e35c36";
    if (intensity > 0.4) return "#f3a156";
    if (intensity > 0.2) return "#f7d4a3";

    return "#f9e9d3";
  }

  return (
    <div className="history-section">

      <h2>Historical Fire Activity</h2>

      <p>Select a date to explore its detections.</p>

      <div className="history-calendar">

        {history.map(day => (
          <button
            key={day.date}
            onClick={() => setSelectedDate(day.date)}
            className="history-day"
            style={{
              backgroundColor: getActivityColor(
                day.totalCount
              ),
              color: day.totalCount / maxCount > 0.6
                ? "white"
                : "#333",
              border: selectedDate === day.date
                ? "3px solid #333"
                : "3px solid transparent"
            }}
          >
            <strong>{day.date}</strong>

            <span>
              {day.totalCount.toLocaleString()}
            </span>
          </button>
        ))}

      </div>

    </div>
  );
}

export default HistoryCalendar;
